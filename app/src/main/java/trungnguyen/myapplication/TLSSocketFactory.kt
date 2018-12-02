package trungnguyen.myapplication

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

//import timber.log.Timber;

class TLSSocketFactory : SSLSocketFactory() {

    private var mSSLSocketFactory: SSLSocketFactory? = null

    init {
        try {
            val context = SSLContext.getInstance("TLS")
            context.init(null, null, java.security.SecureRandom())
            mSSLSocketFactory = context.socketFactory
        } catch (e: NoSuchAlgorithmException) {
            //            Timber.d(e.getMessage());
        } catch (e: KeyManagementException) {
            //            Timber.d(e.getMessage());
        }

    }

    override fun getDefaultCipherSuites(): Array<String> {
        return mSSLSocketFactory!!.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return mSSLSocketFactory!!.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(): Socket? {
        return enableTLSOnSocket(mSSLSocketFactory!!.createSocket())
    }

    @Throws(IOException::class)
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket? {
        return enableTLSOnSocket(mSSLSocketFactory!!.createSocket(s, host, port, autoClose))
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket? {
        return enableTLSOnSocket(mSSLSocketFactory!!.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket? {
        return enableTLSOnSocket(mSSLSocketFactory!!.createSocket(host, port, localHost, localPort))
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket? {
        return enableTLSOnSocket(mSSLSocketFactory!!.createSocket(host, port))
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket? {
        return enableTLSOnSocket(mSSLSocketFactory!!.createSocket(address, port, localAddress, localPort))
    }

    private fun enableTLSOnSocket(socket: Socket?): Socket? {
        if (socket != null && socket is SSLSocket) {
            socket.enabledProtocols = arrayOf("TLSv1.2")
        }
        return socket
    }
}
