package com.embercrown.launcher

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder

/** Small cross-platform progress window; headless launcher tests simply skip it. */
internal class UpdateWindow(private val canPlayInstalled: Boolean) {
    val cancelled = AtomicBoolean(false)
    private var frame: JFrame? = null
    private var status: JLabel? = null
    private var progress: JProgressBar? = null
    private val enabled = !GraphicsEnvironment.isHeadless()

    fun open() {
        if (!enabled) return
        onEdt {
            val text = JLabel("Checking for Embercrown updates…")
            val bar = JProgressBar(0, 100).apply { isIndeterminate = true; isStringPainted = true }
            val panel = JPanel(BorderLayout(0, 12)).apply {
                border = EmptyBorder(18, 20, 18, 20)
                add(text, BorderLayout.NORTH)
                add(bar, BorderLayout.CENTER)
                if (canPlayInstalled) {
                    add(JButton("Play installed version").apply {
                        addActionListener { cancelled.set(true) }
                    }, BorderLayout.SOUTH)
                }
            }
            frame = JFrame("Embercrown Updater").apply {
                defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
                addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(event: WindowEvent) {
                        if (canPlayInstalled) cancelled.set(true)
                    }
                })
                contentPane = panel
                minimumSize = Dimension(350, 125)
                pack()
                setLocationRelativeTo(null)
                isVisible = true
            }
            status = text
            progress = bar
        }
    }

    fun downloading(version: Version) {
        if (!enabled) return
        SwingUtilities.invokeLater {
            status?.text = "Downloading Embercrown $version…"
            progress?.isIndeterminate = false
            progress?.value = 0
        }
    }

    fun progress(done: Long, total: Long) {
        if (!enabled || total <= 0L) return
        SwingUtilities.invokeLater {
            progress?.value = ((done * 100L / total).toInt()).coerceIn(0, 100)
            status?.text = "Downloading Embercrown… ${done / 1_048_576} / ${total / 1_048_576} MB"
        }
    }

    fun verifying() {
        if (!enabled) return
        SwingUtilities.invokeLater {
            status?.text = "Verifying and installing update…"
            progress?.isIndeterminate = true
        }
    }

    fun close() {
        if (!enabled) return
        onEdt {
            frame?.dispose()
            frame = null
            status = null
            progress = null
        }
    }

    private fun onEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) action() else SwingUtilities.invokeAndWait(action)
    }
}
