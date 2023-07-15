import com.materialkolor.demo.App
import org.jetbrains.skiko.wasm.onWasmReady

fun main() {
    onWasmReady {
        BrowserViewportWindow("demo") {
            App()
        }
    }
}
