package com.pbrane.mike.ipv4subnet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
//import android.os.AsyncTask;
//import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
//import android.text.Editable;
import android.text.Html;
import android.graphics.Typeface;
//import android.util.Log;
//import android.text.TextWatcher;
import android.util.TypedValue;
//import android.text.Spanned;
//import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
//import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.NumberFormat;
//import java.util.Formatter;


public class MainActivity extends Activity {

	public static final int MAX_RANGES = 32;
    private CalculateSubnet subnet = new CalculateSubnet();
    TextView textView;
	EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);
		textView.setTypeface(Typeface.MONOSPACE);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
		textView.setTextColor(Color.WHITE);

		editText = (EditText) findViewById(R.id.editText);

		if (savedInstanceState != null) { // restore saved state
			String ipAddr = savedInstanceState.getString("IPAddr");
//			editText = (EditText) findViewById(R.id.editText);
			editText.setText(ipAddr);
			validateAndCalculateSubnet(ipAddr);
		} else { // get the last IP/mask used and insert in editText
			SharedPreferences sharedPref = this.getPreferences(MODE_PRIVATE);
			String ipAddr = sharedPref.getString(getString(R.string.savedIP), "");
//			editText = (EditText) findViewById(R.id.editText);
			editText.setText(ipAddr);
		}

		// hide the softkeyboard when the textview is clicked
		textView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				switch (v.getId()) {
					case R.id.textView:
						HideSoftKeyboard();
						break;
				}
			}
		});
    }

	@Override
	public void onSaveInstanceState(@NonNull Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);

//		editText = (EditText) findViewById(R.id.editText);
		String ipAddr = editText.getText().toString();
		savedInstanceState.putString("IPAddr", ipAddr);
	}

	@Override
	public void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);

		String ipAddr = savedInstanceState.getString("IPAddr");
//		EditText editText = (EditText) findViewById(R.id.editText);
		editText.setText(ipAddr);
		validateAndCalculateSubnet(ipAddr);
	}

	// use the enter key to start the process
	@Override
	public boolean dispatchKeyEvent(@NonNull KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
			processEntry();
			return true;
		}
		return super.dispatchKeyEvent(e);
	}

	public void HideSoftKeyboard()
	{
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
	}

	public void processEntry()
	{
//		EditText editText = (EditText) findViewById(R.id.editText);
		final String IpAndMask = editText.getText().toString();
		saveIP(IpAndMask);
		validateAndCalculateSubnet(IpAndMask);
	}

	// save the IP/mask to the prefs file
	public void saveIP(String ip)
	{
		SharedPreferences sharedPrefs = this.getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putString(getString(R.string.savedIP), ip);
		editor.apply();
	}

	// 1. check for and validate CIDR notation
	// 2. check for and validate Classful notation
	// 3. calculate subnet info
	// 4. display results
	public void validateAndCalculateSubnet(final String ipAddr)
	{
//		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
//			private ProgressBar pb = (ProgressBar) findViewById(R.id.pbCalc);
//			boolean failed = false;
//
//			@Override
//			protected void onPreExecute()
//			{
//				if (pb != null) {
//					pb.setVisibility(ProgressBar.VISIBLE);
//				}
//			}
//
//			@Override
//			protected Void doInBackground(Void... arg0)
//			{
//				try {
//					if (subnet.validateCIDR(ipAddr)) { // handle CIDR notation
//						subnet.calculateSubnetCIDR(ipAddr);
//					} else if (subnet.validateIPAndMaskOctets(ipAddr)) { // handle IP with mask octets notation
//						subnet.calculateSubnetCIDR(subnet.convertToCIDR(ipAddr));
//					} else if (subnet.validateIPAddress(ipAddr)) {
//						subnet.calculateSubnetCIDR(ipAddr + "/32");
//					}
//				} catch (Exception e) {
//					Log.e("CalculateSubnet", "Failed to calculate the subnet");
//					failed = true;
//				}
//				return null;
//			}
//
//			@Override
//			protected void onPostExecute(Void result)
//			{
//				if (pb != null) {
//					pb.setVisibility(ProgressBar.INVISIBLE);
//				}
//				if (failed) {
//					displayError();
//				} else {
//					displayResults();
//				}
//			}
//		};
//		task.execute((Void[])null);
		if (subnet.validateCIDR(ipAddr)) { // handle CIDR notation
			subnet.calculateSubnetCIDR(ipAddr);
		} else if (subnet.validateIPAndMaskOctets(ipAddr)) { // handle IP with mask octets notation
			subnet.calculateSubnetCIDR(subnet.convertToCIDR(ipAddr));
		} else if (subnet.validateIPAddress(ipAddr)) {
			subnet.calculateSubnetCIDR(ipAddr + "/32");
		} else {
			displayError();
			return;
		}
		displayResults();
	}

	// 'Clr' button callback
    public void on_clr(View view)
    {
//		EditText editText = (EditText) findViewById(R.id.editText);
		editText.setText("");
    }

//	public Spanned format(String title, String result, String color1, String color2)
//	{
//		String html = "<b><font color=" + color1 + ">" + title + ": </b><font color=" + color2 + ">" + result + "</font>";
//		return Html.fromHtml(html);
//	}

//	public String format(String title, String result)
//	{
////		Formatter fmt = new Formatter();
////		fmt.format("%s: %15s\n", title, result);
////		return fmt.toString();
//		return String.format("%s:%-15s\n", title, result);
//	}

//    public void drawText(String title, String result, boolean newline)
//    {
////        textView.append(format(title, result, "#000000", "#CC0000")/*Html.fromHtml(html)*/);
////        if (newline) {
////            textView.append("\n");
////        }
//		textView.append(format(title, result));
//    }

//    public void drawTextWithColors(String title, String result, String color1, String color2, boolean newline)
//    {
////		textView.append(format(title, result, color1, color2));
////        if (newline) {
////            textView.append("\n");
////        }
//		textView.append(format(title, result));
//    }

//	public void drawTextBold(String text, String color, boolean newline)
//	{
////		String html = "<font color=" + color + "><b>" + text + "</b></font>";
////		textView.append(Html.fromHtml(html));
////		if (newline) {
////			textView.append("\n");
////		}
//		textView.append(format(text, ""));
//	}

	protected void displayLogo()
	{
		String logoString = "<small><font color=#4169E1><b>IPv4</font><font color=#00CC00>Subnet\u00A0-\u00A0</b></font>"
				+ "<font color=#FFFFFF><u><b>Michael</b></u></font>"
				+ "<font color=#C50000><u>Sheppard</u></font>"
				+ "<font color=#4169E1>\u00A0-\u00A0<b>2015</b></font>\n";

		textView.append("\n");
		textView.append("--------------------------\n");

		textView.append(Html.fromHtml(logoString));
	}

//    public void lineBreak(s

	public void displayError()
	{
		textView.setText("");
		textView.append("ERROR: Invalid IP Address / Mask bits\n");
//		drawTextWithColors("ERROR: ", "Invalid IP Address / Mask bits", "#0000CC", "#CC0000", true);
	}

    public void displayResults()
    {
        textView.setText(""); // clear TextView
//		Formatter String = new Formatter();

		textView.append("[Classful]\n");
//		drawTextBold("[Classful]", "#640000", true);
		String hostIP = subnet.getIpAddr();
		textView.append(String.format("%-25s%s\n", "Host Address:", hostIP));
//		drawText("Host Address", hostIP, true);
		textView.append(String.format("%-25s%s\n", "Host Address (decimal):", subnet.getIpAddrDecimal()));
//		drawText("Host Address (decimal)", subnet.getIpAddrDecimal(), true);
		textView.append(String.format("%-25s%s\n", "Host Address (hex):", subnet.getIpAddrHex()));
//		drawText("Host Address (hex)", subnet.getIpAddrHex(), true);
		textView.append(String.format("%-25s%s\n", "Network Class:", subnet.getNetworkClass(hostIP)));
//		drawText("Network Class", subnet.getNetworkClass(hostIP), true);
		String baseNetwork = subnet.getBaseNetwork(hostIP);
		String baseNetworkMask = subnet.getNetworkClassMask(baseNetwork);
		textView.append(String.format("%-25s%s\n", "Network Address:", baseNetwork));
//		drawText("Network Address", baseNetwork, true);
		textView.append(String.format("%-25s%s\n", "Network Mask:", baseNetworkMask));
//		drawText("Network Mask", baseNetworkMask, true);
		textView.append(String.format("%-25s%s\n", "Network Mask (hex):", subnet.ipToHex(baseNetworkMask)));
//		drawText("Network Mask (hex)", subnet.ipToHex(baseNetworkMask), true);
		textView.append(String.format("%-25s%s\n", "Broadcast:", subnet.getNetworkClassBroadcast(baseNetwork)));
//		drawText("Broadcast", subnet.getNetworkClassBroadcast(baseNetwork), true);
		String nHosts = NumberFormat.getNumberInstance().format(subnet.calcHostsForNetworkClass(baseNetwork) - 2);
		textView.append(String.format("%-25s%s\n", "Number of hosts:", nHosts));
		textView.append("\n");
//		drawText("Number of hosts", NumberFormat.getNumberInstance().format(subnet.calcHostsForNetworkClass(baseNetwork) - 2), true);
//		lineBreak(false);

		textView.append("[CIDR]\n");
//		drawTextBold("[CIDR]", "#640000", true);
		textView.append(String.format("%-25s%s\n", "Host Address:", hostIP));
//		drawText("Host Address", hostIP, true);
		textView.append(String.format("%-25s%s\n", "Host Address (decimal):", subnet.getIpAddrDecimal()));
//		drawText("Host Address (decimal)", subnet.getIpAddrDecimal(), true);
		textView.append(String.format("%-25s%s\n", "Host Address (hex):", subnet.getIpAddrHex()));
//		drawText("Host Address (hex)", subnet.getIpAddrHex(), true);
		textView.append(String.format("%-25s%s\n", "Network Address:", subnet.getNetwork()));
//		drawText("Network Address", subnet.getNetwork(), true);
		String mask = subnet.getDecimalMaskOctets();
		textView.append(String.format("%-25s%s\n", "NetMask:", mask));
//		drawText("Network Mask", mask, true);
		String nhbits = Integer.toString(subnet.getNetworkBits()) + " / " +Integer.toString(subnet.getHostBits());
		textView.append(String.format("%-25s%s\n", "Net/Host Mask (bits):", nhbits));
//        drawText("Network/Host Mask (bits)", nhbits, true);
		textView.append(String.format("%-25s%s\n", "NetMask (hex):", subnet.ipToHex(mask)));
//		drawText("Network Mask (hex)", subnet.ipToHex(mask), true);
		textView.append(String.format("%-25s%s\n", "Broadcast:", subnet.getBroadcast()));
//		drawText("Broadcast", subnet.getBroadcast(), true);
		textView.append(String.format("%-25s%s\n", "Cisco Wildcard:", subnet.getWildcard()));
//        drawText("Cisco Wildcard", subnet.getWildcard(), true);
		String utips = Integer.toString(subnet.getUsableHosts()) + " / " + Integer.toString(subnet.getNumberOfAddresses());
		textView.append(String.format("%-25s%s\n", "Usable/Total IPs:", utips));
//        drawText("Usable/Total IP Addresses", utips, true);
		textView.append(String.format("%-9s%s - %s\n", "Network:", subnet.getNetwork(),subnet.getBroadcast()));
//		drawText("Network", subnet.getNetwork() + " - " + subnet.getBroadcast(), true);
		textView.append(String.format("%-9s%s - %s\n", "Usable:", subnet.getMinHostAddr(), subnet.getMaxHostAddr()));
//		drawText("Usable", subnet.getMinHostAddr() + " - " + subnet.getMaxHostAddr(), true);
		String nSubnets = NumberFormat.getNumberInstance().format(subnet.getAvailableSubnets());
		textView.append(String.format("%-25s%s\n", "Available Subnets:", nSubnets));
		textView.append("\n");
//		drawText("Available Subnets", NumberFormat.getNumberInstance().format(subnet.getAvailableSubnets()), true);
//        lineBreak(false);
		textView.append("[Classful Bitmaps]\n");
//		drawTextBold("[Classfull bitmaps]", "#640000", true);
		textView.append(String.format("%-10s%s\n", "Address:", subnet.ipToBinary(baseNetwork, true)));
//		drawText("Address", subnet.ipToBinary(baseNetwork, true), true);
		textView.append(String.format("%-10s%s\n", "Mask:", subnet.ipToBinary(baseNetworkMask, true)));
//		drawText("Mask", subnet.ipToBinary(baseNetworkMask, true), true);
		textView.append("\n");
//		lineBreak(false);

		textView.append("[CIDR Bitmaps]\n");
//		drawTextBold("[CIDR bitmaps]", "#640000", true);
		textView.append(String.format("%-10s%s\n", "Host IP:", subnet.ipToBinary(hostIP, true)));
//		drawText("Host IP", subnet.ipToBinary(hostIP, true), true);
		textView.append(String.format("%-10s%s\n", "Net IP:", subnet.ipToBinary(baseNetwork, true)));
//		drawText("Net IP", subnet.ipToBinary(baseNetwork, true), true);
		textView.append(String.format("%-10s%s\n", "Netmask:", subnet.ipToBinary(mask, true)));
//		drawText("Netmask", subnet.ipToBinary(mask, true), true);
		textView.append(String.format("%-10s%s\n", "Brdcast:", subnet.ipToBinary(subnet.getBroadcast(), true)));
//		drawText("Brdcast", subnet.ipToBinary(subnet.getBroadcast(), true), true);
		textView.append(String.format("%-10s%s\n", "Wildcard:", subnet.ipToBinary(subnet.getWildcard(), true)));
//		drawText("wildcard", subnet.ipToBinary(subnet.getWildcard(), true), true);
		textView.append("\n");
//		lineBreak(false);
//
		textView.append("[Networks]\n");
//		drawTextBold("[Networks]", "#640000", true);
		String[] ranges = subnet.getRanges();
//		int nRanges = ranges.length;
//		String pad = nRanges >= 100 ? "000" : nRanges > 10 && nRanges < 100 ? "00" : "0"; // padding for the network number
		int count = 1;
		for (String range : ranges) {
//			// add a leading zero, formatting digit widths won't work
//			String strCount = pad + Integer.toString(count);
//			String str = strCount.substring(strCount.length() - pad.length(), strCount.length()) + ". Network";

			String low = range.split(" - ")[0];
			String high = range.split(" - ")[1];
			if (range != null && low.equals(subnet.getNetwork())) {
				textView.append(String.format("%3d. %15s - %s <==\n", count, low, high));
//				drawTextWithColors(str, range, "#000000", "#006400", true);
			} else {
				textView.append(String.format("%3d. %15s - %s\n", count, low, high));
//				drawTextWithColors(str, range, "#000000", "#cc0000", true);
			}
			count++;
		}
		displayLogo();
		HideSoftKeyboard();
    }
}
