const express = require('express');
const router = express.Router();
const attendanceController = require('../controllers/attendanceController');
const { protect } = require('../middleware/auth');
const { uploadSelfie } = require('../middleware/upload');

router.use(protect);

router.post('/checkin',       uploadSelfie.single('selfie'), attendanceController.checkIn);
router.post('/checkout',      uploadSelfie.single('selfie'), attendanceController.checkOut);
router.get('/today',          attendanceController.getTodayAttendance);
router.get('/active-session', attendanceController.getActiveSession);
router.get('/history',        attendanceController.getHistory);

module.exports = router;