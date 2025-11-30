package com.sevengroup.artifyme.managers;

import com.sevengroup.artifyme.models.EditorState;
import java.util.Stack;

public class HistoryManager {
    private final Stack<EditorState> undoStack = new Stack<>();
    private final Stack<EditorState> redoStack = new Stack<>();

    public void saveState(EditorState state) {
        if (state != null) {
            undoStack.push(state);
            redoStack.clear();
        }
    }

    public EditorState undo(EditorState currentState) {
        if (canUndo()) {
            redoStack.push(currentState);
            return undoStack.pop();
        }
        return null;
    }

    public EditorState redo(EditorState currentState) {
        if (canRedo()) {
            undoStack.push(currentState);
            return redoStack.pop();
        }
        return null;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}