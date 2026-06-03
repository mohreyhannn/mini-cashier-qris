package com.example.minicashier

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager

class UsbPrinterHelper(
    private val context: Context
) {

    fun getConnectedPrinter(): UsbDevice? {

        val usbManager =
            context.getSystemService(Context.USB_SERVICE) as UsbManager

        val devices =
            usbManager.deviceList.values

        return devices.firstOrNull()
    }
}