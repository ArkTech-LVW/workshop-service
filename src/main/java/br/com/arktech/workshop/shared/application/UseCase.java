package br.com.arktech.workshop.shared.application;

import org.jspecify.annotations.Nullable;

public interface UseCase<I, O extends @Nullable Object> {

    O execute(I input);
}
