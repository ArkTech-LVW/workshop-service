package br.com.arktech.workshop.shared.application;

import org.jspecify.annotations.Nullable;

// O is @Nullable Void for use cases with no output.
public interface UseCase<I, O extends @Nullable Object> {

    O execute(I input);
}
