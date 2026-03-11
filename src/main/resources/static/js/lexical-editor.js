/**
 * Plain Text Editor Integration
 * Vanilla JavaScript contenteditable-based implementation (plain text only, no formatting)
 */

/**
 * Initialize a plain text editor
 * @param {string} editorId - The ID of the container element for the editor
 * @param {string} hiddenInputId - The ID of the hidden input to store plain text content
 * @param {string} initialContent - Optional initial plain text content
 * @returns {Promise<Object>} The editor instance with methods: setContent, getContent, focus
 */
async function initLexicalEditor(editorId, hiddenInputId, initialContent = null) {
  const editorContainer = document.getElementById(editorId);
  const hiddenInput = document.getElementById(hiddenInputId);

  if (!editorContainer || !hiddenInput) {
    console.error(`Editor or input not found: ${editorId}, ${hiddenInputId}`);
    return null;
  }

  console.log('Initializing plain text editor:', editorId);

  try {
    // If already wrapped, remove the wrapper first to prevent duplicates
    if (editorContainer.parentNode?.classList.contains('editor-container')) {
      const wrapper = editorContainer.parentNode;
      const originalParent = wrapper.parentNode;
      originalParent.insertBefore(editorContainer, wrapper);
      wrapper.remove();
    }

    // Create wrapper without toolbar
    const wrapper = document.createElement('div');
    wrapper.className = 'editor-container';

    // Insert wrapper before the editor container
    editorContainer.parentNode.insertBefore(wrapper, editorContainer);

    // Move editor container into wrapper (no toolbar)
    wrapper.appendChild(editorContainer);

    // Setup editor container as contenteditable
    editorContainer.setAttribute('contenteditable', 'true');
    editorContainer.setAttribute('spellcheck', 'true');
    editorContainer.className = 'lexical-editor';
    editorContainer.style.cursor = 'text';

    // Strip HTML and set initial plain text content
    if (initialContent) {
      const plainText = stripHtmlTags(initialContent);
      editorContainer.textContent = plainText;
    }

    // Sync editor changes to hidden input (plain text only)
    const updateHiddenInput = () => {
      const plainText = editorContainer.textContent || '';
      hiddenInput.value = plainText;
    };

    // Handle paste events - convert pasted HTML to plain text
    editorContainer.addEventListener('paste', (e) => {
      e.preventDefault();

      // Get pasted content as plain text
      const pastedText = (e.clipboardData || window.clipboardData).getData('text/plain');

      // Insert plain text at cursor position
      if (window.getSelection) {
        const sel = window.getSelection();
        if (sel.getRangeAt && sel.rangeCount) {
          const range = sel.getRangeAt(0);
          range.deleteContents();

          const textNode = document.createTextNode(pastedText);
          range.insertNode(textNode);
          range.setStartAfter(textNode);
          range.setEndAfter(textNode);
          sel.removeAllRanges();
          sel.addRange(range);
        }
      }

      // Update hidden input after paste
      setTimeout(updateHiddenInput, 10);
    });

    // Listen for changes
    editorContainer.addEventListener('input', updateHiddenInput);
    editorContainer.addEventListener('change', updateHiddenInput);
    editorContainer.addEventListener('blur', updateHiddenInput);

    // Prevent any HTML tags from being created (intercept beforeinput)
    editorContainer.addEventListener('beforeinput', (e) => {
      if (e.inputType === 'insertHTML' || e.inputType === 'insertFromPaste') {
        e.preventDefault();
      }
    });

    // Initial sync
    updateHiddenInput();

    // Return API object
    const api = {
      container: editorContainer,
      setContent: (text) => {
        const plainText = stripHtmlTags(text);
        editorContainer.textContent = plainText;
        updateHiddenInput();
      },
      getContent: () => hiddenInput.value,
      focus: () => editorContainer.focus(),
      editor: null,
    };

    console.log('Plain text editor initialized:', editorId);
    return api;
  } catch (error) {
    console.error('Failed to initialize editor:', error);
    console.error('Error stack:', error.stack);
    return null;
  }
}

/**
 * Strip HTML tags from text, converting to plain text
 * @param {string} html - HTML content to convert
 * @returns {string} Plain text content
 */
function stripHtmlTags(html) {
  if (!html) return '';

  // Create a temporary element to parse HTML safely
  const temp = document.createElement('div');
  temp.innerHTML = html;

  // Get text content (automatically strips tags)
  return temp.textContent || temp.innerText || '';
}

// Export to global scope
window.LexicalEditor = {
  init: initLexicalEditor,
};
