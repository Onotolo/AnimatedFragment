package my.onotolo.android.animatedfragment.utils

import android.view.View

internal fun View.addSimpleOnLayoutChangeListener(listener: () -> Unit) {
    this.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
        override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
            listener()
            v?.removeOnLayoutChangeListener(this)
        }
    })
}