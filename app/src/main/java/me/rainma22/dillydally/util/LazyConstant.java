package me.rainma22.dillydally.util;

import java.util.function.Supplier;

public class LazyConstant<T> implements Supplier<T> {
    private T instance = null;
    private Supplier<T> supply;
    
    public LazyConstant(Supplier<T> supply1){
        this.supply = supply1;
    };
    
    @Override
    public T get() {
        if(instance == null){
            instance = supply.get();
        }
        return instance;
    }
    
    
}
