package br.com.arktech.workshop.shared.application;

public interface UseCase<I, O> {

    O execute(I input);
}
