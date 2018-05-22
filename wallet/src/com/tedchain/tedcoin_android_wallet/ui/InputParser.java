/*
 * Copyright 2013-2014 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tedchain.tedcoin_android_wallet.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.DialogInterface.OnClickListener;

import com.tedchain.tedcoinj.core.Address;
import com.tedchain.tedcoinj.core.AddressFormatException;
import com.tedchain.tedcoinj.core.Base58;
import com.tedchain.tedcoinj.core.DumpedPrivateKey;
import com.tedchain.tedcoinj.core.ECKey;
import com.tedchain.tedcoinj.core.NetworkParameters;
import com.tedchain.tedcoinj.core.ProtocolException;
import com.tedchain.tedcoinj.core.Transaction;
import com.tedchain.tedcoinj.protocols.payments.PaymentRequestException;
import com.tedchain.tedcoinj.protocols.payments.PaymentRequestException.PkiVerificationException;
import com.tedchain.tedcoinj.uri.tedcoinURI;
import com.tedchain.tedcoinj.uri.tedcoinURIParseException;

import com.tedchain.tedcoin_android_wallet.Constants;
import com.tedchain.tedcoin_android_wallet.data.PaymentIntent;
import com.tedchain.tedcoin_android_wallet.util.Io;
import com.tedchain.tedcoin_android_wallet.util.PaymentProtocol;
import com.tedchain.tedcoin_android_wallet.util.Qr;
import com.tedchain.tedcoin_android_wallet.R;

/**
 * @author Andreas Schildbach
 */
public abstract class InputParser
{
	private static final Logger log = LoggerFactory.getLogger(InputParser.class);

	public abstract static class StringInputParser extends InputParser
	{
		private final String input;

		public StringInputParser(@Nonnull final String input)
		{
			this.input = input;
		}

		@Override
		public void parse()
		{
			if (input.startsWith("ppcoin:-"))
			{
				try
				{
					final byte[] serializedPaymentRequest = Qr.decodeBinary(input.substring(9));

					parseAndHandlePaymentRequest(serializedPaymentRequest);
				}
				catch (final IOException x)
				{
					log.info("i/o error while fetching payment request", x);

					error(R.string.input_parser_io_error, x.getMessage());
				}
				catch (final PkiVerificationException x)
				{
					log.info("got unverifyable payment request", x);

					error(R.string.input_parser_unverifyable_paymentrequest, x.getMessage());
				}
				catch (final PaymentRequestException x)
				{
					log.info("got invalid payment request", x);

					error(R.string.input_parser_invalid_paymentrequest, x.getMessage());
				}
			}
			else if (input.startsWith("ppcoin:"))
			{
				try
				{
					final tedcoinURI tedcoinUri = new tedcoinURI(null, input);

					final Address address = tedcoinUri.getAddress();
					if (address == null)
						throw new tedcoinURIParseException("missing address");

					if (address.getParameters().equals(Constants.NETWORK_PARAMETERS))
						handlePaymentIntent(PaymentIntent.fromtedcoinUri(tedcoinUri));
					else
						error(R.string.input_parser_invalid_address, input);
				}
				catch (final tedcoinURIParseException x)
				{
					log.info("got invalid tedcoin uri: '" + input + "'", x);

					error(R.string.input_parser_invalid_tedcoin_uri, input);
				}
			}
			else if (PATTERN_tedcoin_ADDRESS.matcher(input).matches())
			{
				try
				{
					final Address address = new Address(Constants.NETWORK_PARAMETERS, input);

					handlePaymentIntent(PaymentIntent.fromAddress(address, null));
				}
				catch (final AddressFormatException x)
				{
					log.info("got invalid address", x);

					error(R.string.input_parser_invalid_address);
				}
			}
			else if (PATTERN_PRIVATE_KEY.matcher(input).matches())
			{
				try
				{
					final ECKey key = new DumpedPrivateKey(Constants.NETWORK_PARAMETERS, input).getKey();

					handlePrivateKey(key);
				}
				catch (final AddressFormatException x)
				{
					log.info("got invalid address", x);

					error(R.string.input_parser_invalid_address);
				}
			}
			else if (PATTERN_TRANSACTION.matcher(input).matches())
			{
				try
				{
					final Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, Qr.decodeDecompressBinary(input));

					handleDirectTransaction(tx);
				}
				catch (final IOException x)
				{
					log.info("i/o error while fetching transaction", x);

					error(R.string.input_parser_invalid_transaction, x.getMessage());
				}
				catch (final ProtocolException x)
				{
					log.info("got invalid transaction", x);

					error(R.string.input_parser_invalid_transaction, x.getMessage());
				}
			}
			else
			{
				cannotClassify(input);
			}
		}
	}

	public abstract static class BinaryInputParser extends InputParser
	{
		private final String inputType;
		private final byte[] input;

		public BinaryInputParser(@Nonnull final String inputType, @Nonnull final byte[] input)
		{
			this.inputType = inputType;
			this.input = input;
		}

		@Override
		public void parse()
		{
			if (Constants.MIMETYPE_TRANSACTION.equals(inputType))
			{
				try
				{
					final Transaction tx = new Transaction(Constants.NETWORK_PARAMETERS, input);

					handleDirectTransaction(tx);
				}
				catch (final ProtocolException x)
				{
					log.info("got invalid transaction", x);

					error(R.string.input_parser_invalid_transaction, x.getMessage());
				}
			}
			else if (PaymentProtocol.MIMETYPE_PAYMENTREQUEST.equals(inputType))
			{
				try
				{
					parseAndHandlePaymentRequest(input);
				}
				catch (final PkiVerificationException x)
				{
					log.info("got unverifyable payment request", x);

					error(R.string.input_parser_unverifyable_paymentrequest, x.getMessage());
				}
				catch (final PaymentRequestException x)
				{
					log.info("got invalid payment request", x);

					error(R.string.input_parser_invalid_paymentrequest, x.getMessage());
				}
			}
			else
			{
				cannotClassify(inputType);
			}
		}

		@Override
		protected final void handlePrivateKey(@Nonnull final ECKey key)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected final void handleDirectTransaction(@Nonnull final Transaction transaction)
		{
			throw new UnsupportedOperationException();
		}
	}

	public abstract static class StreamInputParser extends InputParser
	{
		private final String inputType;
		private final InputStream is;

		public StreamInputParser(@Nonnull final String inputType, @Nonnull final InputStream is)
		{
			this.inputType = inputType;
			this.is = is;
		}

		@Override
		public void parse()
		{
			if (PaymentProtocol.MIMETYPE_PAYMENTREQUEST.equals(inputType))
			{
				ByteArrayOutputStream baos = null;

				try
				{
					baos = new ByteArrayOutputStream();
					Io.copy(is, baos);
					parseAndHandlePaymentRequest(baos.toByteArray());
				}
				catch (final IOException x)
				{
					log.info("i/o error while fetching payment request", x);

					error(R.string.input_parser_io_error, x.getMessage());
				}
				catch (final PkiVerificationException x)
				{
					log.info("got unverifyable payment request", x);

					error(R.string.input_parser_unverifyable_paymentrequest, x.getMessage());
				}
				catch (final PaymentRequestException x)
				{
					log.info("got invalid payment request", x);

					error(R.string.input_parser_invalid_paymentrequest, x.getMessage());
				}
				finally
				{
					try
					{
						if (baos != null)
							baos.close();
					}
					catch (IOException x)
					{
						x.printStackTrace();
					}

					try
					{
						is.close();
					}
					catch (IOException x)
					{
						x.printStackTrace();
					}
				}
			}
			else
			{
				cannotClassify(inputType);
			}
		}

		@Override
		protected final void handlePrivateKey(@Nonnull final ECKey key)
		{
			throw new UnsupportedOperationException();
		}

		@Override
		protected final void handleDirectTransaction(@Nonnull final Transaction transaction)
		{
			throw new UnsupportedOperationException();
		}
	}

	public abstract void parse();

	protected final void parseAndHandlePaymentRequest(@Nonnull final byte[] serializedPaymentRequest) throws PaymentRequestException
	{
		final PaymentIntent paymentIntent = PaymentProtocol.parsePaymentRequest(serializedPaymentRequest);

		handlePaymentIntent(paymentIntent);
	}

	protected abstract void handlePaymentIntent(@Nonnull PaymentIntent paymentIntent);

	protected void handlePrivateKey(@Nonnull final ECKey key)
	{
		final Address address = new Address(Constants.NETWORK_PARAMETERS, key.getPubKeyHash());

		handlePaymentIntent(PaymentIntent.fromAddress(address, null));
	}

	protected abstract void handleDirectTransaction(@Nonnull Transaction transaction);

	protected abstract void error(int messageResId, Object... messageArgs);

	protected void cannotClassify(@Nonnull final String input)
	{
		error(R.string.input_parser_cannot_classify, input);
	}

	protected void dialog(final Context context, @Nullable final OnClickListener dismissListener, final int titleResId, final int messageResId,
			final Object... messageArgs)
	{
		final DialogBuilder dialog = new DialogBuilder(context);
		if (titleResId != 0)
			dialog.setTitle(titleResId);
		dialog.setMessage(context.getString(messageResId, messageArgs));
		dialog.singleDismissButton(dismissListener);
		dialog.show();
	}

	private static final Pattern PATTERN_tedcoin_ADDRESS = Pattern.compile("[" + new String(Base58.ALPHABET) + "]{20,40}");
	private static final Pattern PATTERN_PRIVATE_KEY = Pattern.compile("7" + "[" + new String(Base58.ALPHABET) + "]{50}");
	private static final Pattern PATTERN_TRANSACTION = Pattern.compile("[0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$\\*\\+\\-\\.\\/\\:]{100,}");
}
