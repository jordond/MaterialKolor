import androidx.compose.ui.window.ComposeUIViewController
import com.materialkolor.demo.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    return ComposeUIViewController { App() }
}
