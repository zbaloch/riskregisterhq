/**
 * Color utility for generating consistent colors from strings
 */
window.ColorUtils = {
  // TailwindCSS color palette for asset types
  colors: [
    { bg: 'bg-blue-50', text: 'text-blue-700', border: 'border-blue-100' },
    { bg: 'bg-indigo-50', text: 'text-indigo-700', border: 'border-indigo-100' },
    { bg: 'bg-purple-50', text: 'text-purple-700', border: 'border-purple-100' },
    { bg: 'bg-pink-50', text: 'text-pink-700', border: 'border-pink-100' },
    { bg: 'bg-orange-50', text: 'text-orange-700', border: 'border-orange-100' },
    { bg: 'bg-amber-50', text: 'text-amber-700', border: 'border-amber-100' },
    { bg: 'bg-teal-50', text: 'text-teal-700', border: 'border-teal-100' },
    { bg: 'bg-cyan-50', text: 'text-cyan-700', border: 'border-cyan-100' },
    { bg: 'bg-emerald-50', text: 'text-emerald-700', border: 'border-emerald-100' },
    { bg: 'bg-lime-50', text: 'text-lime-700', border: 'border-lime-100' },
  ],

  /**
   * Simple string hash function
   * @param {string} str - The string to hash
   * @returns {number} - A numeric hash
   */
  hashString(str) {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32bit integer
    }
    return Math.abs(hash);
  },

  /**
   * Get a consistent color object for a given string
   * @param {string} str - The string to get color for (e.g., asset type)
   * @returns {object} - Object with bg, text, and border classes
   */
  getColorForString(str) {
    if (!str) return this.colors[0];
    const hash = this.hashString(str);
    const index = hash % this.colors.length;
    return this.colors[index];
  },

  /**
   * Get color classes as a single string for direct use in templates
   * @param {string} str - The string to get color for
   * @returns {string} - Space-separated class names
   */
  getColorClasses(str) {
    const color = this.getColorForString(str);
    return `${color.bg} ${color.text} border ${color.border}`;
  },

  /**
   * Get color for Alpine.js templates (returns object with :class binding)
   * @param {string} str - The string to get color for
   * @returns {object} - Object with class names for Alpine binding
   */
  getColorBinding(str) {
    const color = this.getColorForString(str);
    return {
      [color.bg]: true,
      [color.text]: true,
      'border': true,
      [color.border]: true,
    };
  },
};
