const express = require('express');
const router = express.Router();
const adminController = require('../controllers/adminController');
const { protect, authorize } = require('../middleware/auth');

router.use(protect);
router.use(authorize('admin'));

router.get('/users',                 adminController.getAllUsers);
router.post('/users',                adminController.addUser);
router.delete('/users/:id',          adminController.deleteUser);
router.get('/attendance',            adminController.getAllAttendance);
router.get('/export/attendance/csv', adminController.exportAttendanceCSV);
router.get('/export/attendance/pdf', adminController.exportAttendancePDF);

module.exports = router;