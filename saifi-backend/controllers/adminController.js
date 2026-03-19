const Attendance = require('../models/Attendance');
const User       = require('../models/User');
const moment     = require('moment-timezone');
const PDFDocument = require('pdfkit');
const IST        = 'Asia/Kolkata';

const toIST = (ts) => moment(ts).tz(IST).format('HH:mm:ss');
const toISTDate = (ts) => moment(ts).tz(IST).format('YYYY-MM-DD');

// GET /api/admin/users
exports.getAllUsers = async (req, res) => {
  try {
    const users = await User.find().select('-password').sort({ name: 1 });
    res.json({ success: true, count: users.length, data: users });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

// POST /api/admin/users
exports.addUser = async (req, res) => {
  try {
    const { employeeId, name, email, password, role } = req.body;
    const exists = await User.findOne({ $or: [{ employeeId }, { email }] });
    if (exists)
      return res.status(400).json({ success: false, message: 'Employee ID or email already exists' });

    const user = await User.create({ employeeId, name, email, password, role: role || 'carpenter' });
    res.status(201).json({
      success: true,
      message: `${name} added successfully`,
      user: { _id: user._id, employeeId: user.employeeId, name: user.name, email: user.email, role: user.role, isActive: user.isActive }
    });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

// DELETE /api/admin/users/:id
exports.deleteUser = async (req, res) => {
  try {
    const user = await User.findById(req.params.id);
    if (!user) return res.status(404).json({ success: false, message: 'User not found' });
    await user.deleteOne();
    res.json({ success: true, message: 'Carpenter removed successfully' });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

// GET /api/admin/attendance  — paired sessions for admin panel
exports.getAllAttendance = async (req, res) => {
  try {
    const { startDate, endDate, employeeId } = req.query;
    let query = {};
    if (startDate && endDate) query.date = { $gte: startDate, $lte: endDate };
    if (employeeId) query.employeeId = employeeId;

    const records = await Attendance.find(query)
      .populate('userId', 'name employeeId email')
      .sort({ timestamp: 1 });

    // Pair check-ins with check-outs by sessionId
    const sessionsMap = {};
    records.forEach(r => {
      const sid = r.sessionId;
      if (!sessionsMap[sid]) sessionsMap[sid] = { checkIn: null, checkOut: null, userId: r.userId };
      if (r.type === 'check_in') sessionsMap[sid].checkIn = r;
      else sessionsMap[sid].checkOut = r;
    });

    const sessions = Object.entries(sessionsMap).map(([sid, s]) => {
      const ci = s.checkIn;
      const co = s.checkOut;
      return {
        _id:              ci ? ci._id : (co?._id),
        userId:           s.userId,
        employeeId:       ci?.employeeId || co?.employeeId,
        sessionId:        sid,
        checkInTime:      ci?.timestamp || null,
        checkOutTime:     co?.timestamp || null,
        checkInSelfieUrl: ci?.selfieUrl || null,
        checkOutSelfieUrl:co?.selfieUrl || null,
        checkInLatitude:  ci?.latitude  || 0,
        checkInLongitude: ci?.longitude || 0,
        checkInAddress:   ci?.address   || '',
        checkOutAddress:  co?.address   || null,
        date:             ci?.date || co?.date,
        isSynced:         true
      };
    }).sort((a, b) => {
      const da = a.checkInTime || a.checkOutTime;
      const db = b.checkInTime || b.checkOutTime;
      return new Date(db) - new Date(da);
    });

    res.json({ success: true, count: sessions.length, data: sessions });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

// GET /api/admin/export/attendance/csv
exports.exportAttendanceCSV = async (req, res) => {
  try {
    const { startDate, endDate, employeeId } = req.query;
    let query = {};
    if (startDate && endDate) query.date = { $gte: startDate, $lte: endDate };
    if (employeeId) query.employeeId = employeeId;

    const records = await Attendance.find(query)
      .populate('userId', 'name employeeId')
      .sort({ date: -1, timestamp: 1 })
      .limit(5000);

    // Group by session
    const sessions = {};
    records.forEach(r => {
      if (!sessions[r.sessionId]) sessions[r.sessionId] = { ci: null, co: null, user: r.userId };
      if (r.type === 'check_in') sessions[r.sessionId].ci = r;
      else sessions[r.sessionId].co = r;
    });

    let csv = 'Carpenter ID,Carpenter Name,Date,Check-In Time,Check-In Address,Check-Out Time,Check-Out Address\n';
    Object.values(sessions).forEach(({ ci, co, user }) => {
      csv += `"${ci?.employeeId || co?.employeeId}",`;
      csv += `"${user?.name || 'Unknown'}",`;
      csv += `"${ci?.date || co?.date}",`;
      csv += `"${ci ? toIST(ci.timestamp) : '-'}",`;
      csv += `"${ci?.address || '-'}",`;
      csv += `"${co ? toIST(co.timestamp) : '-'}",`;
      csv += `"${co?.address || '-'}"\n`;
    });

    const fname = `saifi_attendance_${Date.now()}.csv`;
    res.setHeader('Content-Type', 'text/csv; charset=utf-8');
    res.setHeader('Content-Disposition', `attachment; filename="${fname}"`);
    res.status(200).send(csv);
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

// GET /api/admin/export/attendance/pdf
exports.exportAttendancePDF = async (req, res) => {
  try {
    const { startDate, endDate, employeeId } = req.query;
    let query = {};
    if (startDate && endDate) query.date = { $gte: startDate, $lte: endDate };
    if (employeeId) query.employeeId = employeeId;

    const records = await Attendance.find(query)
      .populate('userId', 'name employeeId')
      .sort({ date: -1, timestamp: 1 })
      .limit(1000);

    const sessions = {};
    records.forEach(r => {
      if (!sessions[r.sessionId]) sessions[r.sessionId] = { ci: null, co: null, user: r.userId };
      if (r.type === 'check_in') sessions[r.sessionId].ci = r;
      else sessions[r.sessionId].co = r;
    });

    const doc = new PDFDocument({ margin: 30, size: 'A4', layout: 'landscape', bufferPages: true });
    const fname = `saifi_attendance_${Date.now()}.pdf`;
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename="${fname}"`);
    doc.pipe(res);

    doc.fontSize(18).font('Helvetica-Bold').text('Saifi Furnitures — Attendance Report', { align: 'center' });
    doc.moveDown(0.5);
    doc.fontSize(11).font('Helvetica').text(`Period: ${startDate || 'All'} to ${endDate || 'All'}`, { align: 'center' });
    doc.fontSize(10).text(`Generated: ${moment().tz(IST).format('DD/MM/YYYY HH:mm')} IST`, { align: 'center' });
    doc.moveDown(1.5);

    const rows = Object.values(sessions);
    const ROW_H = 32;
    let y = doc.y;

    const drawHeader = () => {
      doc.font('Helvetica-Bold').fontSize(8);
      doc.rect(30, y, 750, 20).fill('#6B3A1F');
      doc.fillColor('white');
      doc.text('Carpenter',    35,  y + 6, { width: 90 });
      doc.text('ID',          130,  y + 6, { width: 65 });
      doc.text('Date',        200,  y + 6, { width: 65 });
      doc.text('Check In',    270,  y + 6, { width: 65 });
      doc.text('In Location', 340,  y + 6, { width: 170 });
      doc.text('Check Out',   515,  y + 6, { width: 65 });
      doc.text('Out Location',585,  y + 6, { width: 190 });
      doc.fillColor('black');
      y += 22;
    };

    drawHeader();

    rows.forEach((s, idx) => {
      if (y > 520) { doc.addPage(); y = 40; drawHeader(); }
      const bg = idx % 2 === 0 ? '#FFF9F2' : '#FFFFFF';
      doc.rect(30, y, 750, ROW_H - 2).fill(bg);
      doc.font('Helvetica').fontSize(7).fillColor('#1C0F05');
      doc.text(s.user?.name || 'Unknown',      35,  y + 4, { width: 90 });
      doc.text(s.ci?.employeeId || s.co?.employeeId || '', 130, y + 4, { width: 65 });
      doc.text(s.ci?.date || s.co?.date || '',  200, y + 4, { width: 65 });
      doc.text(s.ci ? toIST(s.ci.timestamp) : '-', 270, y + 4, { width: 65 });
      doc.text(s.ci?.address || '-',            340, y + 4, { width: 170, height: ROW_H - 6, ellipsis: true });
      doc.text(s.co ? toIST(s.co.timestamp) : '-', 515, y + 4, { width: 65 });
      doc.text(s.co?.address || '-',            585, y + 4, { width: 190, height: ROW_H - 6, ellipsis: true });
      y += ROW_H;
    });

    doc.end();
  } catch (err) {
    if (!res.headersSent)
      res.status(500).json({ success: false, message: err.message });
  }
};