const User = require('../models/User');
const jwt  = require('jsonwebtoken');

const generateToken = (id) => {
  return jwt.sign({ id }, process.env.JWT_SECRET, { expiresIn: process.env.JWT_EXPIRE });
};

const login = async (req, res) => {
  try {
    const { employeeId, password } = req.body;

    if (!employeeId || !password) {
      return res.status(400).json({ success: false, message: 'Please provide ID and password' });
    }

    const user = await User.findOne({ employeeId }).select('+password');

    if (!user || !(await user.matchPassword(password))) {
      return res.status(401).json({ success: false, message: 'Invalid credentials' });
    }

    if (!user.isActive) {
      return res.status(401).json({ success: false, message: 'Account deactivated' });
    }

    res.json({
      success: true,
      message: 'Login successful',
      token: generateToken(user._id),
      user: {
        id:         user._id,
        employeeId: user.employeeId,
        name:       user.name,
        email:      user.email,
        role:       user.role
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

const register = async (req, res) => {
  try {
    const { employeeId, name, email, password, role } = req.body;

    const exists = await User.findOne({ $or: [{ email }, { employeeId }] });
    if (exists) {
      return res.status(400).json({ success: false, message: 'Employee ID or email already exists' });
    }

    const user = await User.create({
      employeeId,
      name,
      email,
      password,
      role: role || 'carpenter'
    });

    res.status(201).json({
      success: true,
      message: 'Carpenter registered successfully',
      token: generateToken(user._id),
      user: {
        id:         user._id,
        employeeId: user.employeeId,
        name:       user.name,
        email:      user.email,
        role:       user.role
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

const getMe = async (req, res) => {
  try {
    const user = await User.findById(req.user.id);
    res.json({
      success: true,
      user: {
        id:         user._id,
        employeeId: user.employeeId,
        name:       user.name,
        email:      user.email,
        role:       user.role
      }
    });
  } catch (err) {
    res.status(500).json({ success: false, message: err.message });
  }
};

module.exports = { login, register, getMe };
