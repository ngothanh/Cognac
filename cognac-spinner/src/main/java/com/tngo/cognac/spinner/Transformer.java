package com.tngo.cognac.spinner;

public interface Transformer<I, O> {
    O transform(I input);
}
