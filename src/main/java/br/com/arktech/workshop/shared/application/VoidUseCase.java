package br.com.arktech.workshop.shared.application;

public interface VoidUseCase<I> {

    void execute(I input);
}
