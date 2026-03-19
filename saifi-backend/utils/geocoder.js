const axios = require('axios');

async function getAddressFromCoordinates(latitude, longitude) {
  try {
    if (!latitude || !longitude || isNaN(latitude) || isNaN(longitude))
      return `${latitude}, ${longitude}`;

    // zoom=18 = building level, addressdetails=1 = full breakdown
    const url = `https://us1.locationiq.com/v1/reverse` +
      `?key=${process.env.LOCATIONIQ_API_KEY}` +
      `&lat=${latitude}&lon=${longitude}` +
      `&format=json&addressdetails=1&zoom=18&normalizecity=1`;

    const response = await axios.get(url, { timeout: 10000 });
    const data = response.data;

    if (!data || !data.address) return `${latitude}, ${longitude}`;

    const a = data.address;

    console.log('Geocoder raw address:', JSON.stringify(a));

    const parts = [];

    // 1. Most specific: building/amenity name
    const building = a.amenity || a.building || a.shop || a.office || a.tourism;
    if (building) parts.push(building);

    // 2. House number + road
    if (a.house_number && a.road) {
      parts.push(`${a.house_number}, ${a.road}`);
    } else if (a.road) {
      parts.push(a.road);
    } else if (a.pedestrian) {
      parts.push(a.pedestrian);
    } else if (a.footway) {
      parts.push(a.footway);
    } else if (a.path) {
      parts.push(a.path);
    }

    // 3. Neighbourhood / colony / sector
    const micro = a.neighbourhood || a.quarter || a.subdivision;
    if (micro) parts.push(micro);

    // 4. Suburb / area
    const area = a.suburb || a.residential || a.industrial || a.commercial;
    if (area && area !== micro) parts.push(area);

    // 5. City district / locality
    const locality = a.city_district || a.locality || a.county;
    if (locality) parts.push(locality);

    // 6. City
    const city = a.city || a.town || a.village || a.municipality;
    if (city && !parts.includes(city)) parts.push(city);

    // Return if we have at least 2 meaningful parts
    if (parts.length >= 2) return parts.join(', ');

    // Fallback: use display_name but trim it to first 4 segments
    if (data.display_name) {
      const segs = data.display_name.split(',').map(s => s.trim()).filter(Boolean);
      // Skip numeric-only segments and country
      const filtered = segs.filter(s => !/^\d+$/.test(s) && s !== 'India').slice(0, 4);
      if (filtered.length >= 2) return filtered.join(', ');
    }

    return `${latitude}, ${longitude}`;

  } catch (err) {
    console.error('Geocoding error:', err.message);
    return `${latitude}, ${longitude}`;
  }
}

module.exports = { getAddressFromCoordinates };