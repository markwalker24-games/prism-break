package app.prismbreak;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.getcapacitor.BridgeActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Allow the window to extend into the camera-cutout area; we pad the web
        // content back out of it explicitly below.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        enableImmersive();
        applyInsetsAsPadding();
        applyGestureExclusion();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersive();
            applyGestureExclusion();
        }
    }

    /**
     * Fully immersive (both bars hidden) so the game keeps its fullscreen look.
     * BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE makes an edge swipe just *reveal* the
     * bars transiently instead of firing the system back / home gesture — which is
     * what was hijacking the left/right hold controls.
     */
    private void enableImmersive() {
        // Edge-to-edge (enforced on modern Android; the framework no longer fits
        // content for us). We inset the content ourselves in applyInsetsAsPadding().
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        View decor = getWindow().getDecorView();
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), decor);
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    /**
     * On modern Android edge-to-edge is enforced and the framework no longer
     * auto-pads content for the bars/cutout. Do it explicitly by padding the
     * activity content view (padding a parent reliably resizes the web viewport,
     * unlike padding the WebView itself which can just clip): push content below
     * the camera cutout / status bar and fill to the bottom (nav bar hidden -> its
     * inset is 0). This shifts the whole game down out from under the camera and
     * removes the matching gap at the bottom.
     */
    private void applyInsetsAsPadding() {
        final View content = findViewById(android.R.id.content);
        if (content == null) return;
        content.setBackgroundColor(Color.parseColor("#0a0a0f"));
        ViewCompat.setOnApplyWindowInsetsListener(content, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                Insets safe = insets.getInsets(
                        WindowInsetsCompat.Type.statusBars()
                                | WindowInsetsCompat.Type.navigationBars()
                                | WindowInsetsCompat.Type.displayCutout());
                v.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return insets;
            }
        });
        ViewCompat.requestApplyInsets(content);
    }

    /**
     * Reinforce immersive by telling the system not to treat gestures on the
     * left/right screen edges as system gestures. Android caps gesture exclusion
     * at ~200dp per edge, so we cover the vertical band where thumbs rest.
     */
    private void applyGestureExclusion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        final View root = getWindow().getDecorView();
        root.post(new Runnable() {
            @Override
            public void run() {
                int w = root.getWidth();
                int h = root.getHeight();
                if (w == 0 || h == 0) return;
                float density = getResources().getDisplayMetrics().density;
                int band = (int) Math.min(h, 200 * density); // exclusion capped at 200dp/edge
                int top = (h - band) / 2;
                int edge = (int) (28 * density);              // ~28dp inward from each side
                List<Rect> rects = new ArrayList<>();
                rects.add(new Rect(0, top, edge, top + band));        // left edge
                rects.add(new Rect(w - edge, top, w, top + band));    // right edge
                root.setSystemGestureExclusionRects(rects);
            }
        });
    }
}
