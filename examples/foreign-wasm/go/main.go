package main

//go:wasmexport add
func add(a, b int32) int32 { return a + b }

//go:wasmexport fib
func fib(n int32) int64 {
	a, b := int64(0), int64(1)
	for i := int32(0); i < n; i++ {
		a, b = b, a+b
	}
	return a
}

func main() {}
