const NodeGeocoder = require('node-geocoder');

const geocoder = NodeGeocoder({
  provider: 'locationiq',
  apiKey:   process.env.LOCATIONIQ_API_KEY,
  formatter: null
});

async function getAddressFromCoordinates(latitude, longitude) {
  try {
    if (!latitude || !longitude || isNaN(latitude) || isNaN(longitude))
      return `${latitude}, ${longitude}`;

    const res = await geocoder.reverse({ lat: parseFloat(latitude), lon: parseFloat(longitude) });

    if (res && res.length > 0) {
      const loc = res[0];
      const parts = [
        loc.neighbourhood || loc.suburb || loc.locality,
        loc.city || loc.town || loc.village || loc.county,
        loc.state || loc.stateDistrict,
        loc.country
      ].filter(Boolean);

      return parts.length >= 2
        ? parts.join(', ')
        : (loc.formattedAddress || loc.display_name || `${latitude}, ${longitude}`);
    }
    return `${latitude}, ${longitude}`;
  } catch (err) {
    console.error('Geocoding error:', err.message);
    return `${latitude}, ${longitude}`;
  }
}

module.exports = { getAddressFromCoordinates };