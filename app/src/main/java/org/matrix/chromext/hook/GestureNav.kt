package org.matrix.chromext.hook

// import android.annotation.TargetApi
// import android.graphics.Rect
// import android.os.Build
// import android.view.ViewGroup
import android.content.Context
import com.github.kyuubiran.ezxhelper.utils.findMethod
import com.github.kyuubiran.ezxhelper.utils.hookBefore
import org.matrix.chromext.proxy.GestureNavProxy

object GestureNavHook : BaseHook() {
  override fun init(ctx: Context) {

    val proxy = GestureNavProxy(ctx)

    findMethod(proxy.historyNavigationCoordinator!!) { name == proxy.IS_FEATURE_ENABLED }
        // private boolean isFeatureEnabled()
        .hookBefore {
          it.setResult(true)
          it.result = true
        }

    // disableSystemGesture(proxy)
  }

  // @TargetApi(11)
  // fun disableSystemGesture(proxy: GestureNavProxy) {
  //   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
  //     findMethod(proxy.sideSlideLayout!!) { name == "onLayout" }
  //         // protected void onLayout(boolean changed, int left, int top, int right, int bottom)
  //         .hookAfter {
  //           val view = it.thisObject as ViewGroup
  //           val left = it.args[1] as Int
  //           val top = it.args[2] as Int
  //           val right = it.args[3] as Int
  //           val bottom = it.args[4] as Int
  //           view.setSystemGestureExclusionRects(
  //               // public Rect (int left, int top, int right, int bottom)
  //               listOf(Rect(0, 0, Math.abs(right - left) + 1000, Math.abs(top - bottom))))
  //         }
  //   }
  // }
}
