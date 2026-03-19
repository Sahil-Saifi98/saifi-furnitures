const express   = require('express');
const dotenv    = require('dotenv');
const cors      = require('cors');
const connectDB = require('./config/database');

dotenv.config();
connectDB();

const app = express();

app.use((req, res, next) => { req.setTimeout(600000); res.setTimeout(600000); next(); });
app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

app.use('/api/auth',       require('./routes/auth'));
app.use('/api/attendance', require('./routes/attendance'));
app.use('/api/admin',      require('./routes/admin'));

app.get('/', (req, res) => res.json({
  success: true,
  message: 'Saifi Furnitures API is running',
  version: '1.0.0',
  endpoints: { auth: '/api/auth', attendance: '/api/attendance', admin: '/api/admin' }
}));

app.use((req, res) => res.status(404).json({ success: false, message: 'Route not found' }));
app.use((err, req, res, next) => res.status(500).json({ success: false, message: err.message || 'Server Error' }));

const PORT   = process.env.PORT || 5001;
const server = app.listen(PORT, () => {
  console.log(`🪵 Saifi Furnitures API running on port ${PORT}`);
  console.log(`📡 Environment: ${process.env.NODE_ENV}`);
});

server.timeout          = 600000;
server.keepAliveTimeout = 610000;
server.headersTimeout   = 620000;

module.exports = app;