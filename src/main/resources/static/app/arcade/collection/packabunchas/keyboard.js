(() => {
  let pieceIndex = -1;
  let buttonIndex = -1;
  let selected = null;

  function mouseEvent(x, y) {
    const rect = c.getBoundingClientRect();
    return {
      clientX: rect.left + x * rect.width / c.width,
      clientY: rect.top + y * rect.height / c.height
    };
  }

  function focusPoint() {
    if (state === 'game' && gameStarted && !isCinematic) {
      const piece = pieces[pieceIndex];
      if (!piece) return null;
      return {
        x: piece.originalPosition.x + piece.blocks[0][0] * blockSize,
        y: piece.originalPosition.y + piece.blocks[0][1] * blockSize
      };
    }
    return buttons.filter(button => button.x > 0 && button.x < w && button.y > 0 && button.y < h)[buttonIndex];
  }

  function keyboardFocus() {
    const point = focusPoint();
    if (point) mouseMove(mouseEvent(point.x, point.y));
  }

  const originalDraw = draw;
  draw = function () {
    originalDraw();
    const point = focusPoint();
    if (!point) return;
    ctx.save();
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 6;
    ctx.strokeRect(point.x - 24, point.y - 24, 48, 48);
    ctx.restore();
  };

  window.addEventListener('pointerdown', () => {
    buttonIndex = -1;
    pieceIndex = -1;
    selected = null;
  }, true);

  window.addEventListener('keydown', event => {
    const key = event.key;
    if (!['Tab', 'Enter', ' ', 'ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'r', 'R'].includes(key)) return;
    const inPuzzle = state === 'game' && gameStarted && !isCinematic && pieces.length > 0;
    const choices = inPuzzle ? pieces : buttons.filter(button => button.x > 0 && button.x < w && button.y > 0 && button.y < h);
    if (key === 'Tab') {
      if (!choices.length) return;
      event.preventDefault();
      if (selected) return;
      const current = inPuzzle ? pieceIndex : buttonIndex;
      const next = current < 0 ? (event.shiftKey ? choices.length - 1 : 0) :
        (current + (event.shiftKey ? -1 : 1) + choices.length) % choices.length;
      if (inPuzzle) pieceIndex = next; else buttonIndex = next;
      keyboardFocus();
      return;
    }
    if (key === 'Enter' || key === ' ') {
      event.preventDefault();
      if (event.repeat) return;
      if (!inPuzzle) {
        const point = focusPoint() || choices[0] || { x: w / 2, y: h / 2 };
        const input = mouseEvent(point.x, point.y);
        mouseDown(input);
        mouseUp(input);
        buttonIndex = -1;
        return;
      }
      if (pieceIndex < 0 || !pieces[pieceIndex]) pieceIndex = 0;
      if (selected) {
        mouseUp(mouseEvent(mouseX, mouseY));
        selected = null;
      } else {
        selected = pieces[pieceIndex];
        selected.color = selected.pickColor;
        selected.isDragged = true;
        draggedBlock = selected;
      }
      return;
    }
    const piece = selected || pieces[pieceIndex];
    if (!inPuzzle || !piece) return;
    event.preventDefault();
    if (key === 'r' || key === 'R') {
      if (!event.repeat) piece.rotate();
      return;
    }
    if (!selected) return;
    const dx = Number(key === 'ArrowRight') - Number(key === 'ArrowLeft');
    const dy = Number(key === 'ArrowDown') - Number(key === 'ArrowUp');
    const nextX = selected.originalPosition.x + dx * blockSize;
    const nextY = selected.originalPosition.y + dy * blockSize;
    if (selected.blocks.every(([x, y]) => nextX + x * blockSize >= 0 && nextX + x * blockSize <= w &&
        nextY + y * blockSize >= 0 && nextY + y * blockSize <= h)) {
      selected.originalPosition.x = nextX;
      selected.originalPosition.y = nextY;
    }
  });
})();
