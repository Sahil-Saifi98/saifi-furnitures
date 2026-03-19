// Run: node seed.js
// Creates only the admin account — carpenters are added via the Admin Panel

require('dotenv').config();
const mongoose = require('mongoose');
const User     = require('./models/User');

async function seed() {
  try {
    await mongoose.connect(process.env.MONGODB_URI);
    console.log('✅ Connected to MongoDB');

    const exists = await User.findOne({ employeeId: 'ADMIN-001' });
    if (exists) {
      console.log('⏭️  Admin already exists — nothing to do');
      process.exit(0);
    }

    await User.create({
      employeeId: 'ADMIN-001',
      name:       'Saifi Admin',
      email:      'admin@saifi.com',
      password:   'admin123',
      role:       'admin'
    });

    console.log('✅ Admin created successfully');
    console.log('\n📋 Admin login:');
    console.log('   ID:       ADMIN-001');
    console.log('   Password: admin123');
    console.log('\n👉 Login to the app and add carpenters from the Admin Panel → Carpenters');
    process.exit(0);
  } catch (err) {
    console.error('❌ Seed error:', err.message);
    process.exit(1);
  }
}

seed();