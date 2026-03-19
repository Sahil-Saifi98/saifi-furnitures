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

    if (!employeeId || !name || !password)
      return res.status(400).json({ success: false, message: 'Employee ID, name and password are required' });

    const orConditions = [{ employeeId }];
    if (email && email.trim() !== '') orConditions.push({ email: email.toLowerCase().trim() });
    const exists = await User.findOne({ $or: orConditions });
    if (exists)
      return res.status(400).json({ success: false, message: 'Employee ID or email already exists' });

    const userData = { employeeId, name, password, role: role || 'carpenter' };
    if (email && email.trim() !== '') userData.email = email.toLowerCase().trim();
    const user = await User.create(userData);
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
    // Step 1: pair by exact sessionId match
    const sessionsMap = {};
    records.forEach(r => {
      const sid = r.sessionId;
      if (!sessionsMap[sid]) sessionsMap[sid] = { checkIn: null, checkOut: null, userId: r.userId };
      if (r.type === 'check_in') sessionsMap[sid].checkIn = r;
      else sessionsMap[sid].checkOut = r;
    });

    // Step 2: for orphaned check-outs (no matching check-in by sessionId),
    // try to pair with orphaned check-ins from the same user on the same date
    const orphanedCheckOuts = Object.values(sessionsMap)
      .filter(s => s.checkOut !== null && s.checkIn === null);
    const orphanedCheckIns  = Object.values(sessionsMap)
      .filter(s => s.checkIn !== null && s.checkOut === null);

    orphanedCheckOuts.forEach(coSession => {
      const co = coSession.checkOut;
      const coDate = co.date;
      const coUser = co.userId?._id?.toString() || co.employeeId;

      // Find best matching orphaned check-in: same user, same date, timestamp before checkout
      const match = orphanedCheckIns.find(ciSession => {
        const ci = ciSession.checkIn;
        const ciUser = ci.userId?._id?.toString() || ci.employeeId;
        return ci.date === coDate &&
               ciUser === coUser &&
               ci.timestamp <= co.timestamp;
      });

      if (match) {
        // Merge: put check-out into the check-in's session, remove the orphaned check-out entry
        match.checkOut = co;
        delete sessionsMap[co.sessionId];
        // Remove from orphanedCheckIns so it can't be matched again
        const idx = orphanedCheckIns.indexOf(match);
        if (idx > -1) orphanedCheckIns.splice(idx, 1);
      }
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

    // Group by session with orphan pairing
    const sessMap = {};
    records.forEach(r => {
      if (!sessMap[r.sessionId]) sessMap[r.sessionId] = { ci: null, co: null, user: r.userId };
      if (r.type === 'check_in') sessMap[r.sessionId].ci = r;
      else sessMap[r.sessionId].co = r;
    });
    const oCOs = Object.values(sessMap).filter(s => s.co && !s.ci);
    const oCIs = Object.values(sessMap).filter(s => s.ci && !s.co);
    oCOs.forEach(coS => {
      const co = coS.co;
      const coUser = co.userId?._id?.toString() || co.employeeId;
      const match = oCIs.find(ciS => {
        const ci = ciS.ci;
        return ci.date === co.date &&
               (ci.userId?._id?.toString() || ci.employeeId) === coUser &&
               ci.timestamp <= co.timestamp;
      });
      if (match) { match.co = co; delete sessMap[co.sessionId]; oCIs.splice(oCIs.indexOf(match), 1); }
    });
    const sessions = Object.values(sessMap);

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

    // Step 1: pair by sessionId
    const sessionsMap = {};
    records.forEach(r => {
      if (!sessionsMap[r.sessionId]) sessionsMap[r.sessionId] = { ci: null, co: null, user: r.userId };
      if (r.type === 'check_in') sessionsMap[r.sessionId].ci = r;
      else sessionsMap[r.sessionId].co = r;
    });

    // Step 2: pair orphaned check-outs with orphaned check-ins (same user, same date)
    const orphanedCOs = Object.values(sessionsMap).filter(s => s.co && !s.ci);
    const orphanedCIs = Object.values(sessionsMap).filter(s => s.ci && !s.co);
    orphanedCOs.forEach(coS => {
      const co = coS.co;
      const coUser = co.userId?._id?.toString() || co.employeeId;
      const match = orphanedCIs.find(ciS => {
        const ci = ciS.ci;
        const ciUser = ci.userId?._id?.toString() || ci.employeeId;
        return ci.date === co.date && ciUser === coUser && ci.timestamp <= co.timestamp;
      });
      if (match) {
        match.co = co;
        delete sessionsMap[co.sessionId];
        orphanedCIs.splice(orphanedCIs.indexOf(match), 1);
      }
    });
    const sessions = Object.values(sessionsMap);

    const doc = new PDFDocument({ margin: 30, size: 'A4', layout: 'landscape', bufferPages: true });
    const fname = `saifi_attendance_${Date.now()}.pdf`;
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename="${fname}"`);
    doc.pipe(res);

    // ── Title ────────────────────────────────────────────────────
    doc.fontSize(18).font('Helvetica-Bold').fillColor('#3B1F0A')
       .text('Saifi Furnitures — Attendance Report', { align: 'center' });
    doc.moveDown(0.3);
    doc.fontSize(10).font('Helvetica').fillColor('#6B3A1F')
       .text(`Period: ${startDate || 'All dates'} to ${endDate || 'All dates'}   |   Generated: ${moment().tz(IST).format('DD/MM/YYYY HH:mm')} IST`, { align: 'center' });
    doc.moveDown(0.3);

    // ── Summary line ─────────────────────────────────────────────
    const rows = sessions;  // already an array
    const completed = rows.filter(s => s.ci && s.co).length;
    const onsite    = rows.filter(s => s.ci && !s.co).length;
    doc.fontSize(9).fillColor('#888')
       .text(`Total: ${rows.length} sessions  |  Completed: ${completed}  |  On Site: ${onsite}`, { align: 'center' });
    doc.moveDown(0.8);

    // ── Column layout (landscape A4 = 841 x 595, margin 30 each side = 781px wide) ──
    // Columns: Carpenter(100) | ID(65) | Date(65) | Check-In Time(60) | Check-In Location(160) | Check-Out Time(60) | Check-Out Location(160) | Status(55)
    const COL = { x: 30, w: 781 };
    const C = {
      name:    { x: 30,  w: 100 },
      empId:   { x: 133, w: 62  },
      date:    { x: 198, w: 62  },
      ciTime:  { x: 263, w: 60  },
      ciLoc:   { x: 326, w: 158 },
      coTime:  { x: 487, w: 60  },
      coLoc:   { x: 550, w: 158 },
      status:  { x: 711, w: 60  },
    };
    const HDR_H = 22;
    const ROW_H = 36;
    let y = doc.y;

    const drawHeader = () => {
      // Header background
      doc.rect(COL.x, y, COL.w, HDR_H).fill('#3B1F0A');
      doc.font('Helvetica-Bold').fontSize(7.5).fillColor('white');
      doc.text('Carpenter',       C.name.x + 2,  y + 7, { width: C.name.w });
      doc.text('Emp ID',          C.empId.x + 2, y + 7, { width: C.empId.w });
      doc.text('Date',            C.date.x + 2,  y + 7, { width: C.date.w });
      doc.text('Check-In',        C.ciTime.x + 2,y + 7, { width: C.ciTime.w });
      doc.text('Check-In Location', C.ciLoc.x + 2, y + 7, { width: C.ciLoc.w });
      doc.text('Check-Out',       C.coTime.x + 2,y + 7, { width: C.coTime.w });
      doc.text('Check-Out Location', C.coLoc.x + 2, y + 7, { width: C.coLoc.w });
      doc.text('Status',          C.status.x + 2,y + 7, { width: C.status.w });
      doc.fillColor('black');
      y += HDR_H + 1;
    };

    drawHeader();

    rows.forEach((s, idx) => {
      // Check if we need a new page (leave room for at least one row)
      if (y + ROW_H > 560) { doc.addPage(); y = 30; drawHeader(); }

      const bg = idx % 2 === 0 ? '#FFF9F2' : '#FFFFFF';
      const isCompleted = s.ci && s.co;
      const statusColor = isCompleted ? '#2E7D32' : '#B8860B';
      const statusText  = isCompleted ? 'Completed' : (s.ci ? 'On Site' : 'Out Only');

      // Row background
      doc.rect(COL.x, y, COL.w, ROW_H - 1).fill(bg);

      // Thin left accent bar
      doc.rect(COL.x, y, 3, ROW_H - 1).fill(isCompleted ? '#4A7C59' : '#D4A853');

      doc.font('Helvetica').fontSize(7.5).fillColor('#1C0F05');

      const ty = y + 4; // text top with padding

      // Carpenter name (bold)
      doc.font('Helvetica-Bold').fontSize(7.5)
         .text(s.user?.name || 'Unknown', C.name.x + 5, ty, { width: C.name.w - 4, ellipsis: true });

      doc.font('Helvetica').fontSize(7).fillColor('#555')
         .text(s.ci?.employeeId || s.co?.employeeId || '', C.name.x + 5, ty + 11, { width: C.name.w - 4 });

      doc.font('Helvetica').fontSize(7.5).fillColor('#1C0F05');

      // Emp ID
      doc.text(s.ci?.employeeId || s.co?.employeeId || '-', C.empId.x + 2, ty, { width: C.empId.w - 2 });

      // Date
      doc.text(s.ci?.date || s.co?.date || '-', C.date.x + 2, ty, { width: C.date.w - 2 });

      // Check-In time (green)
      doc.fillColor(s.ci ? '#2E7D32' : '#aaa')
         .text(s.ci ? toIST(s.ci.timestamp) : '—', C.ciTime.x + 2, ty, { width: C.ciTime.w - 2 });

      // Check-In location
      doc.fillColor('#333')
         .text(s.ci?.address || '—', C.ciLoc.x + 2, ty, {
           width: C.ciLoc.w - 4,
           height: ROW_H - 8,
           ellipsis: true,
           lineBreak: true
         });

      // Check-Out time (red)
      doc.fillColor(s.co ? '#B94040' : '#aaa')
         .text(s.co ? toIST(s.co.timestamp) : '—', C.coTime.x + 2, ty, { width: C.coTime.w - 2 });

      // Check-Out location
      doc.fillColor('#333')
         .text(s.co?.address || '—', C.coLoc.x + 2, ty, {
           width: C.coLoc.w - 4,
           height: ROW_H - 8,
           ellipsis: true,
           lineBreak: true
         });

      // Status badge
      doc.roundedRect(C.status.x + 2, ty - 1, C.status.w - 4, 14, 3).fill(isCompleted ? '#E8F5E9' : '#FFF8E1');
      doc.fillColor(statusColor).font('Helvetica-Bold').fontSize(6.5)
         .text(statusText, C.status.x + 4, ty + 2, { width: C.status.w - 6, align: 'center' });

      // Row bottom border
      doc.moveTo(COL.x, y + ROW_H - 1).lineTo(COL.x + COL.w, y + ROW_H - 1)
         .strokeColor('#E8DDD0').lineWidth(0.5).stroke();

      y += ROW_H;
    });

    // Footer
    doc.moveDown(1);
    doc.font('Helvetica').fontSize(8).fillColor('#aaa')
       .text(`Saifi Furnitures Attendance System — Confidential`, { align: 'center' });

    doc.end();
  } catch (err) {
    if (!res.headersSent)
      res.status(500).json({ success: false, message: err.message });
  }
};