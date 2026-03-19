const multer = require('multer');
const { selfieStorage } = require('../config/cloudinary');

const uploadSelfie = multer({
  storage: selfieStorage,
  limits: { fileSize: 10485760 },
  fileFilter: (req, file, cb) => {
    const allowed = ['image/jpeg','image/jpg','image/png','image/webp','application/octet-stream'];
    allowed.includes(file.mimetype) ? cb(null, true) : cb(new Error('Invalid file type'), false);
  }
});

module.exports = { uploadSelfie };