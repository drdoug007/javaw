//! Plain `extern "C"` exports for wasm32-unknown-unknown: no wasm-bindgen, no imports.

#[no_mangle]
pub extern "C" fn mul(a: i32, b: i32) -> i32 {
    a.wrapping_mul(b)
}

#[no_mangle]
pub extern "C" fn hypot(a: f64, b: f64) -> f64 {
    (a * a + b * b).sqrt()
}

/// Reserves `len` bytes in this module's memory for the host to fill; release them with `dealloc`.
#[no_mangle]
pub extern "C" fn alloc(len: usize) -> *mut u8 {
    let mut buffer = Vec::<u8>::with_capacity(len);
    let pointer = buffer.as_mut_ptr();
    std::mem::forget(buffer);
    pointer
}

#[no_mangle]
pub extern "C" fn dealloc(pointer: *mut u8, len: usize) {
    unsafe { drop(Vec::from_raw_parts(pointer, 0, len)) }
}

/// Counts ASCII vowels in UTF-8 text that the host copied into this module's memory.
#[no_mangle]
pub extern "C" fn count_vowels(pointer: *const u8, len: usize) -> i32 {
    let text = unsafe { std::slice::from_raw_parts(pointer, len) };
    text.iter().filter(|b| b"aeiouAEIOU".contains(b)).count() as i32
}
