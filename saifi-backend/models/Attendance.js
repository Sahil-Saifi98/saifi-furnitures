const mongoose = require('mongoose');

const attendanceSchema = new mongoose.Schema({
  userId:     { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  employeeId: { type: String, required: true },

  // Each check-in/check-out pair shares the same sessionId
  sessionId:  { type: String, required: true },
  type:       { type: String, enum: ['check_in', 'check_out'], required: true },

  selfieUrl:  { type: String, required: true },
  latitude:   { type: Number, required: true },
  longitude:  { type: Number, required: true },
  address:    { type: String, default: '' },

  timestamp:  { type: Date, required: true },
  date:       { type: String, required: true },   // YYYY-MM-DD
  time:       { type: String, required: true },   // HH:mm:ss IST

  isSynced:   { type: Boolean, default: true },
  createdAt:  { type: Date, default: Date.now }
});

attendanceSchema.index({ userId: 1, date: -1 });
attendanceSchema.index({ employeeId: 1, date: -1 });
attendanceSchema.index({ sessionId: 1 });

module.exports = mongoose.model('Attendance', attendanceSchema);