def parametrized(dec):
    """
    example
    @parametrized
    def my_decorator_with_extra_args(func, arg1, arg2):
        ...
    """
    def layer(*args, **kwargs):
        def actual(f):
            return dec(f, *args, **kwargs)
        return actual
    return layer
