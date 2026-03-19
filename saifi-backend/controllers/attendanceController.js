const Attendance = require('../models/Attendance');
const moment     = require('moment-timezone');
const { getAddressFromCoordinates } = require('../utils/geocoder');
const { cloudinary } = require('../config/cloudinary');
const IST = 'Asia/Kolkata';

// Cloudinary upload with retry
const uploadToCloudinary = async (file, retries = 3) => {
  for (let i = 1; i <= retries; i++) {
    try {
      if (file.path?.startsWith('http')) return file.path;
      const result = await new Promise((resolve, reject) => {
        const stream = cloudinary.uploader.upload_stream(
          { folder: 'saifi/selfies', transformation: [{ width: 600, height: 600, crop: 'limit' }, { quality: 'auto:low' }], timeout: 120000 },
          (err, res) => err ? reject(err) : resolve(res)
        );
        stream.end(file.buffer);
      });
      return result.secure_url;
    } catch (err) {
      if (i === retries) throw err;
      await new Promise(r => setTimeout(r, i * 1000));
    }
  }
};

// POST /api/attendance/checkin
exports.checkIn = async (req, res) => {
  try {
    const { latitude, longitude, timestamp, sessionId } = req.body;
    if (!req.file) return res.status(400).json({ success: false, message: 'Selfie required' });

    const lat = parseFloat(latitude);
    const lng = parseFloat(longitude);

    let selfieUrl;
    try { selfieUrl = await uploadToCloudinary(req.file); }
    catch { return res.status(500).json({ success: false, message: 'Failed to upload selfie' }); }

    let address = `${lat}, ${lng}`;
    try { address = await getAddressFromCoordinates(lat, lng); } catch {}

    const ts   = new Date(parseInt(timestamp));
    const date = moment(ts).tz(IST).format('YYYY-MM-DD');
    const time = moment(ts).tz(IST).format('HH:mm:ss');

    // Use provided sessionId from app, or generate one
    const sid  = sessionId || require('crypto').randomUUID();

    const record = await Attendance.create({
      userId:     req.user._id,
      employeeId: req.user.employeeId,
      sessionId:  sid,
      type:       'check_in',
      selfieUrl,
      latitude:   lat,
      longitude:  lng,
      address,
      timestamp:  ts,
      date,
      time
    });

    res.status(201).json({ success: true, message: 'Checked in successfully', data: record });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

// POST /api/attendance/checkout
exports.checkOut = async (req, res) => {
  try {
    const { latitude, longitude, timestamp, sessionId } = req.body;
    if (!req.file) return res.status(400).json({ success: false, message: 'Selfie required' });
    if (!sessionId) return res.status(400).json({ success: false, message: 'sessionId required' });

    const lat = parseFloat(latitude);
    const lng = parseFloat(longitude);

    let selfieUrl;
    try { selfieUrl = await uploadToCloudinary(req.file); }
    catch { return res.status(500).json({ success: false, message: 'Failed to upload selfie' }); }

    let address = `${lat}, ${lng}`;
    try { address = await getAddressFromCoordinates(lat, lng); } catch {}

    const ts   = new Date(parseInt(timestamp));
    const date = moment(ts).tz(IST).format('YYYY-MM-DD');
    const time = moment(ts).tz(IST).format('HH:mm:ss');

    const record = await Attendance.create({
      userId:     req.user._id,
      employeeId: req.user.employeeId,
      sessionId,
      type:       'check_out',
      selfieUrl,
      latitude:   lat,
      longitude:  lng,
      address,
      timestamp:  ts,
      date,
      time
    });

    res.status(201).json({ success: true, message: 'Checked out successfully', data: record });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

// GET /api/attendance/today
exports.getTodayAttendance = async (req, res) => {
  try {
    const today = moment().tz(IST).format('YYYY-MM-DD');
    const records = await Attendance.find({ userId: req.user._id, date: today }).sort({ timestamp: 1 });
    res.json({ success: true, count: records.length, data: records });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

// GET /api/attendance/active-session
exports.getActiveSession = async (req, res) => {
  try {
    const today = moment().tz(IST).format('YYYY-MM-DD');

    // Find check-ins today
    const checkIns = await Attendance.find({ userId: req.user._id, date: today, type: 'check_in' });
    // Find check-outs today
    const checkOutSessionIds = (await Attendance.find({ userId: req.user._id, date: today, type: 'check_out' }))
      .map(r => r.sessionId);

    // Open = check-in with no matching check-out
    const open = checkIns.find(ci => !checkOutSessionIds.includes(ci.sessionId));

    if (open) {
      res.json({ success: true, data: open });
    } else {
      res.json({ success: true, data: null });
    }
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};