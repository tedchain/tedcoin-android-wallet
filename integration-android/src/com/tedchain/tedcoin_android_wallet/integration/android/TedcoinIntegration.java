package com.tedchain.tedcoin_android_wallet.integration.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

/**
 * @author Andreas Schildbach
 */
public final class TedcoinIntegration
{
	private static final String INTENT_EXTRA_PAYMENTREQUEST = "paymentrequest";
	private static final String INTENT_EXTRA_PAYMENT = "payment";
	private static final String INTENT_EXTRA_TRANSACTION_HASH = "transaction_hash";

	private static final String MIMETYPE_PAYMENTREQUEST = "application/tedcoin-paymentrequest"; // BIP 71

	/**
	 * Request any amount of Tedcoin (probably a donation) from user, without feedback from the app.
	 * 
	 * @param context
	 *            Android context
	 * @param address
	 *            Tedcoin address
	 */
	public static void request(final Context context, final String address)
	{
		final Intent intent = makeTedcoinUriIntent(address, null);

		start(context, intent);
	}

	/**
	 * Request specific amount of Tedcoin from user, without feedback from the app.
	 * 
	 * @param context
	 *            Android context
	 * @param address
	 *            Tedcoin address
	 * @param amount
	 *            Tedcoin amount in nanocoins
	 */
	public static void request(final Context context, final String address, final long amount)
	{
		final Intent intent = makeTedcoinUriIntent(address, amount);

		start(context, intent);
	}

	/**
	 * Request payment from user, without feedback from the app.
	 * 
	 * @param context
	 *            Android context
	 * @param paymentRequest
	 *            BIP70 formatted payment request
	 */
	public static void request(final Context context, final byte[] paymentRequest)
	{
		final Intent intent = makePaymentRequestIntent(paymentRequest);

		start(context, intent);
	}

	/**
	 * Request any amount of Tedcoin (probably a donation) from user, with feedback from the app. Result intent can be
	 * received by overriding {@link android.app.Activity#onActivityResult()}. Result indicates either
	 * {@link Activity#RESULT_OK} or {@link Activity#RESULT_CANCELED}. In the success case, use
	 * {@link #transactionHashFromResult(Intent)} to read the transaction hash from the intent.
	 * 
	 * Warning: A success indication is no guarantee! To be on the safe side, you must drive your own Tedcoin
	 * infrastructure and validate the transaction.
	 * 
	 * @param activity
	 *            Calling Android activity
	 * @param requestCode
	 *            Code identifying the call when {@link android.app.Activity#onActivityResult()} is called back
	 * @param address
	 *            Tedcoin address
	 */
	public static void requestForResult(final Activity activity, final int requestCode, final String address)
	{
		final Intent intent = makeTedcoinUriIntent(address, null);

		startForResult(activity, requestCode, intent);
	}

	/**
	 * Request specific amount of Tedcoins from user, with feedback from the app. Result intent can be received by
	 * overriding {@link android.app.Activity#onActivityResult()}. Result indicates either {@link Activity#RESULT_OK} or
	 * {@link Activity#RESULT_CANCELED}. In the success case, use {@link #transactionHashFromResult(Intent)} to read the
	 * transaction hash from the intent.
	 * 
	 * Warning: A success indication is no guarantee! To be on the safe side, you must drive your own Tedcoin
	 * infrastructure and validate the transaction.
	 * 
	 * @param activity
	 *            Calling Android activity
	 * @param requestCode
	 *            Code identifying the call when {@link android.app.Activity#onActivityResult()} is called back
	 * @param address
	 *            Tedcoin address
	 */
	public static void requestForResult(final Activity activity, final int requestCode, final String address, final long amount)
	{
		final Intent intent = makeTedcoinUriIntent(address, amount);

		startForResult(activity, requestCode, intent);
	}

	/**
	 * Request payment from user, with feedback from the app. Result intent can be received by overriding
	 * {@link android.app.Activity#onActivityResult()}. Result indicates either {@link Activity#RESULT_OK} or
	 * {@link Activity#RESULT_CANCELED}. In the success case, use {@link #transactionHashFromResult(Intent)} to read the
	 * transaction hash from the intent.
	 * 
	 * Warning: A success indication is no guarantee! To be on the safe side, you must drive your own Tedcoin
	 * infrastructure and validate the transaction.
	 * 
	 * @param activity
	 *            Calling Android activity
	 * @param requestCode
	 *            Code identifying the call when {@link android.app.Activity#onActivityResult()} is called back
	 * @param paymentRequest
	 *            BIP70 formatted payment request
	 */
	public static void requestForResult(final Activity activity, final int requestCode, final byte[] paymentRequest)
	{
		final Intent intent = makePaymentRequestIntent(paymentRequest);

		startForResult(activity, requestCode, intent);
	}

	/**
	 * Get payment request from intent. Meant for usage by applications accepting payment requests.
	 * 
	 * @param intent
	 *            intent
	 * @return payment request or null
	 */
	public static byte[] paymentRequestFromIntent(final Intent intent)
	{
		final byte[] paymentRequest = intent.getByteArrayExtra(INTENT_EXTRA_PAYMENTREQUEST);

		return paymentRequest;
	}

	/**
	 * Put BIP70 payment message into result intent. Meant for usage by Tedcoin wallet applications.
	 * 
	 * @param result
	 *            result intent
	 * @param payment
	 *            payment message
	 */
	public static void paymentToResult(final Intent result, final byte[] payment)
	{
		result.putExtra(INTENT_EXTRA_PAYMENT, payment);
	}

	/**
	 * Get BIP70 payment message from result intent. Meant for usage by applications initiating a Tedcoin payment.
	 * 
	 * You can use the transactions contained in the payment to validate the payment. For this, you need your own
	 * Tedcoin infrastructure though. There is no guarantee that the payment will ever confirm.
	 * 
	 * @param result
	 *            result intent
	 * @return payment message
	 */
	public static byte[] paymentFromResult(final Intent result)
	{
		final byte[] payment = result.getByteArrayExtra(INTENT_EXTRA_PAYMENT);

		return payment;
	}

	/**
	 * Put transaction hash into result intent. Meant for usage by Tedcoin wallet applications.
	 * 
	 * @param result
	 *            result intent
	 * @param txHash
	 *            transaction hash
	 */
	public static void transactionHashToResult(final Intent result, final String txHash)
	{
		result.putExtra(INTENT_EXTRA_TRANSACTION_HASH, txHash);
	}

	/**
	 * Get transaction hash from result intent. Meant for usage by applications initiating a Tedcoin payment.
	 * 
	 * You can use this hash to request the transaction from the Tedcoin network, in order to validate. For this, you
	 * need your own Tedcoin infrastructure though. There is no guarantee that the transaction has ever been broadcasted
	 * to the Tedcoin network.
	 * 
	 * @param result
	 *            result intent
	 * @return transaction hash
	 */
	public static String transactionHashFromResult(final Intent result)
	{
		final String txHash = result.getStringExtra(INTENT_EXTRA_TRANSACTION_HASH);

		return txHash;
	}

	private static final int NANOCOINS_PER_COIN = 100000000;

	private static Intent makeTedcoinUriIntent(final String address, final Long amount)
	{
		final StringBuilder uri = new StringBuilder("ppcoin:");
		if (address != null)
			uri.append(address);
		if (amount != null)
			uri.append("?amount=").append(String.format("%d.%08d", amount / NANOCOINS_PER_COIN, amount % NANOCOINS_PER_COIN));

		final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri.toString()));

		return intent;
	}

	private static Intent makePaymentRequestIntent(final byte[] paymentRequest)
	{
		final Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setType(MIMETYPE_PAYMENTREQUEST);
		intent.putExtra(INTENT_EXTRA_PAYMENTREQUEST, paymentRequest);

		return intent;
	}

	private static void start(final Context context, final Intent intent)
	{
		final PackageManager pm = context.getPackageManager();
		if (pm.resolveActivity(intent, 0) != null)
			context.startActivity(intent);
		else
			redirectToDownload(context);
	}

	private static void startForResult(final Activity activity, final int requestCode, final Intent intent)
	{
		final PackageManager pm = activity.getPackageManager();
		if (pm.resolveActivity(intent, 0) != null)
			activity.startActivityForResult(intent, requestCode);
		else
			redirectToDownload(activity);
	}

	private static void redirectToDownload(final Context context)
	{
		Toast.makeText(context, "No Tedcoin application found.\nPlease install Tedcoin Wallet.", Toast.LENGTH_LONG).show();

		final Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.tedchain.tedcoin_android_wallet"));
		final Intent binaryIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tedchain/tedcoin-android-wallet"));

		final PackageManager pm = context.getPackageManager();
		if (pm.resolveActivity(marketIntent, 0) != null)
			context.startActivity(marketIntent);
		else if (pm.resolveActivity(binaryIntent, 0) != null)
			context.startActivity(binaryIntent);
		// else out of luck
	}
}
