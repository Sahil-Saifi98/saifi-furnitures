const axios = require('axios');

async function getAddressFromCoordinates(latitude, longitude) {
  try {
    if (!latitude || !longitude || isNaN(latitude) || isNaN(longitude))
      return `${latitude}, ${longitude}`;

    const url = `https://us1.locationiq.com/v1/reverse?key=${process.env.LOCATIONIQ_API_KEY}&lat=${latitude}&lon=${longitude}&format=json&addressdetails=1`;

    const response = await axios.get(url, { timeout: 8000 });
    const data = response.data;

    if (!data || !data.address) return `${latitude}, ${longitude}`;

    const a = data.address;

    // Build precise address: house_number + road + suburb/neighbourhood + city + state
    const parts = [];

    // Street level
    if (a.house_number) parts.push(a.house_number);
    if (a.road || a.pedestrian || a.footway) parts.push(a.road || a.pedestrian || a.footway);

    // Area/locality
    const area = a.neighbourhood || a.suburb || a.quarter || a.residential || a.locality;
    if (area) parts.push(area);

    // City/Town
    const city = a.city || a.town || a.village || a.county || a.district;
    if (city) parts.push(city);

    // State
    if (a.state) parts.push(a.state);

    if (parts.length >= 2) return parts.join(', ');

    // Fallback to display_name shortened
    if (data.display_name) {
      const segments = data.display_name.split(',').map(s => s.trim()).filter(Boolean);
      return segments.slice(0, 4).join(', ');
    }

    return `${latitude}, ${longitude}`;
  } catch (err) {
    console.error('Geocoding error:', err.message);
    return `${latitude}, ${longitude}`;
  }
}

module.exports = { getAddressFromCoordinates };