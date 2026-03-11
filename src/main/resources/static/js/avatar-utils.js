/**
 * Avatar Utilities
 * Generates deterministic colors for user avatars based on name hashes
 */

/**
 * Generate a deterministic hex color based on user name
 * @param {string} name - User name to generate color for
 * @returns {string} Hex color code (e.g., '3B82F6')
 */
function generateAvatarColor(name) {
  if (!name) return '3B82F6'; // Default sky-blue

  // Simple hash function based on string characters
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    const char = name.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32-bit integer
  }

  // Use absolute value and modulo to get a number between 0 and 16777215 (0xFFFFFF)
  const colorIndex = Math.abs(hash) % 16777215;

  // Convert to hex and pad with zeros
  const hexColor = colorIndex.toString(16).padStart(6, '0').toUpperCase();

  return hexColor;
}

/**
 * Get ui-avatars.com URL with deterministic background color
 * @param {string} name - User name for avatar
 * @param {number} size - Avatar size in pixels (default: 32)
 * @returns {string} Complete ui-avatars.com API URL
 */
function getAvatarUrl(name, size = 32) {
  const backgroundColor = generateAvatarColor(name);
  const encodedName = encodeURIComponent(name || 'User');
  return `https://ui-avatars.com/api/?name=${encodedName}&background=random&color=fff&bold=true`;
}

// Export to global scope
window.AvatarUtils = {
  generateAvatarColor,
  getAvatarUrl,
};
