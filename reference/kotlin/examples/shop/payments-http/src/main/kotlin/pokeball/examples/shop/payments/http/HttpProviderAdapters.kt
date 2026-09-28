package pokeball.examples.shop.payments.http

import pokeball.examples.shop.payments.ChargeRequest
import pokeball.examples.shop.payments.ChargeResult
import pokeball.examples.shop.payments.ChargeStatus
import pokeball.examples.shop.payments.StatusRequest
import pokeball.runtime.Adapter
import pokeball.runtime.AdapterResult
import pokeball.runtime.Cancellable
import java.net.ConnectException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletionException

/**
 * HTTP adapters for a card-payment provider with this minimal API:
 *
 * - `POST /charges` with form fields `orderId`, `amount` and an optional
 *   `Idempotency-Key` header → `200 charged:<ref>`, `200 declined`,
 *   `422` (validation error, nothing applied), `5xx` (may have been applied);
 * - `GET /charges?orderId=…` → `200 charged:<ref>` or `200 none`.
 *
 * Contract (Core 2 §9.3): report once, never retry, claim non-application only
 * when the connection was refused before the request was written or the provider
 * answered 422, and send the call identity as idempotency key when asked to.
 */
class HttpProviderAdapters(
    private val baseUrl: URI,
    private val sendIdempotencyKey: Boolean,
    private val client: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build(),
) {
    val charge = Adapter<ChargeRequest, ChargeResult> { inv, done ->
        val remaining = (inv.deadlineAt - System.currentTimeMillis()).coerceAtLeast(1)
        val builder = HttpRequest.newBuilder(baseUrl.resolve("/charges"))
            .timeout(Duration.ofMillis(remaining))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form("orderId" to inv.request.orderId, "amount" to inv.request.amount.toString())))
        if (sendIdempotencyKey) builder.header("Idempotency-Key", inv.callId.toString())
        send(builder.build(), done) { status, body ->
            when {
                status == 200 && body.startsWith("charged:") -> AdapterResult.Ok(ChargeResult.Charged(body.removePrefix("charged:")))
                status == 200 && body == "declined" -> AdapterResult.Ok(ChargeResult.Declined)
                status == 422 -> AdapterResult.NotApplied("provider rejected the request: $body")
                else -> AdapterResult.Failed("HTTP $status") // a 5xx may have been applied
            }
        }
    }

    val status = Adapter<StatusRequest, ChargeStatus> { inv, done ->
        val remaining = (inv.deadlineAt - System.currentTimeMillis()).coerceAtLeast(1)
        val request = HttpRequest.newBuilder(baseUrl.resolve("/charges?" + form("orderId" to inv.request.orderId)))
            .timeout(Duration.ofMillis(remaining))
            .GET()
            .build()
        send(request, done) { status, body ->
            when {
                status == 200 && body.startsWith("charged:") -> AdapterResult.Ok(ChargeStatus.Charged(body.removePrefix("charged:")))
                status == 200 && body == "none" -> AdapterResult.Ok(ChargeStatus.NoCharge)
                else -> AdapterResult.Failed("HTTP $status")
            }
        }
    }

    private fun <T> send(request: HttpRequest, done: (AdapterResult<T>) -> Unit, map: (Int, String) -> AdapterResult<T>): Cancellable {
        val future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        future.whenComplete { response, error ->
            if (error == null) {
                done(map(response.statusCode(), response.body()))
                return@whenComplete
            }
            val cause = (error as? CompletionException)?.cause ?: error
            done(
                when (cause) {
                    // Refused before anything was written: evidence of non-application.
                    is ConnectException, is HttpConnectTimeoutException -> AdapterResult.NotApplied("connection failed: ${cause.message}")
                    // Everything else (timeouts, resets) happened after the request may have left.
                    else -> AdapterResult.Failed("${cause::class.simpleName}: ${cause.message}")
                },
            )
        }
        return Cancellable { future.cancel(true) }
    }

    private fun form(vararg fields: Pair<String, String>) =
        fields.joinToString("&") { (k, v) -> URLEncoder.encode(k, Charsets.UTF_8) + "=" + URLEncoder.encode(v, Charsets.UTF_8) }
}
