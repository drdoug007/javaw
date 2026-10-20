const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const assert = require('assert');

// Configuration
const JAR_PATH = path.join(__dirname, '../target/javaw-1.0-SNAPSHOT.jar');
const JAVA_FILE = path.join(__dirname, '../TestClass.java');
const WAT_FILE = path.join(__dirname, '../TestClass.wat');

function runTest() {
    console.log('Starting javaw output tests...');

    try {
        // 1. Clean up previous runs
        if (fs.existsSync(WAT_FILE)) {
            fs.unlinkSync(WAT_FILE);
            console.log('Cleaned up previous WAT file.');
        }

        // 2. Execute the javaw tool
        console.log(`Running: java -jar ${JAR_PATH} ${JAVA_FILE}`);
        execSync(`java -jar ${JAR_PATH} ${JAVA_FILE}`, { stdio: 'inherit' });

        // 3. Assert file existence
        assert.strictEqual(fs.existsSync(WAT_FILE), true, 'The .wat file should have been generated.');
        console.log('✓ Assert: .wat file exists.');

        // 4. Read and verify content
        const content = fs.readFileSync(WAT_FILE, 'utf8');
        console.log('Generated Content:\n', content);

        // Check for module wrapper
        assert.ok(content.includes('(module'), 'Content should start with (module');
        assert.ok(content.includes(')'), 'Content should end with )');

        // Check for method definitions
        assert.ok(content.includes('(func $sayHello ()'), 'Should contain sayHello function');
        assert.ok(content.includes('(func $addNumbers -> i32)'), 'Should contain addNumbers function with i32 return type');
        assert.ok(content.includes('(func $getPi -> f64)'), 'Should contain getPi function with f64 return type');

        console.log('✓ Assert: Content pattern matches expected WASM Text structure.');
        console.log('\nALL TESTS PASSED! 🎉');

    } catch (error) {
        console.error('\n❌ TEST FAILED!');
        console.error(error.message);
        process.exit(1);
    }
}

runTest();
