package com.pbrane.mike.ipv4subnet;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends Activity {

//	private static final int MAX_HOSTS_TO_DISPLAY = 16;
	public static final int MAX_RANGES = 32;
    private CalculateSubnet subnet = new CalculateSubnet();
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.textView);

		if (savedInstanceState != null) { // restore saved state
			String ipAddr = savedInstanceState.getString("IPAddr");
			EditText editText = (EditText) findViewById(R.id.editText);
			editText.setText(ipAddr);
			validateAndCalculateSubnet(ipAddr);
		} else { // get the last IP/mask used and insert in editText
			SharedPreferences sharedPref = this.getPreferences(MODE_PRIVATE);
			String ipAddr = sharedPref.getString(getString(R.string.savedIP), "");
			EditText editText = (EditText) findViewById(R.id.editText);
			editText.setText(ipAddr);
		}

		// hide the softkeyboard when the textview is clicked
		textView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				HideSoftKeyboard();
			}
		});
    }

	@Override
	public void onSaveInstanceState(@NonNull Bundle savedInstanceState)
	{
		super.onSaveInstanceState(savedInstanceState);

		EditText editText = (EditText) findViewById(R.id.editText);
		String ipAddr = editText.getText().toString();
		savedInstanceState.putString("IPAddr", ipAddr);
	}

	@Override
	public void onRestoreInstanceState(@NonNull Bundle savedInstanceState)
	{
		super.onRestoreInstanceState(savedInstanceState);

		String ipAddr = savedInstanceState.getString("IPAddr");
		EditText editText = (EditText) findViewById(R.id.editText);
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
		EditText editText = (EditText) findViewById(R.id.editText);
		String ipAddr = editText.getText().toString();
		saveIP(ipAddr);
		validateAndCalculateSubnet(ipAddr);
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
	public void validateAndCalculateSubnet(String ipAddr)
	{
		if (subnet.validateCIDR(ipAddr)) { // handle CIDR notation
			subnet.calculateSubnetCIDR(ipAddr);
		} else if (subnet.validateIPAndMaskOctets(ipAddr)) { // handle IP with mask octets notation
			subnet.calculateSubnetCIDR(subnet.convertToCIDR(ipAddr));
		} else {
			textView.setText("");
			formatWithColors("ERROR: ", "Invalid IP Address / Mask bits", "#0000CC", "#CC0000", true);
			return;
		}
		displayResults();
	}

	// 'CALC' button callback
    public void on_calc(View view)
    {
		processEntry();
    }

    public void format(String title, String result, boolean newline)
    {
        String html = "<pre><b><font color=#000000>" + title + ": </b><font color=#CC0000>" + result + "</font></pre>";
        textView.append(Html.fromHtml(html));
        if (newline) {
            textView.append("\n");
        }
    }

    public void formatWithColors(String title, String result, String color1, String color2, boolean newline)
    {
        String html = "<pre><b><font color=" + color1 + ">" + title + ": </b><font color=" + color2 + ">" + result + "</font></pre>";
        textView.append(Html.fromHtml(html));
        if (newline) {
            textView.append("\n");
        }
    }

	public void formatText(String text, String color, boolean bold, boolean newline)
	{
		String boldTag, boldTagEnd;
		if (bold) {
			boldTag = "<b>";
			boldTagEnd = "</b>";
		} else {
			boldTag = "";
			boldTagEnd = "";
		}
		String html = "<pre><font color=" + color + ">" + boldTag + text + boldTagEnd + "</b></font></pre>";
		textView.append(Html.fromHtml(html));
		if (newline) {
			textView.append("\n");
		}
	}

	protected void displayLogo()
	{
		String logoString = "<pre><small><font color=#4169E1><b>IPv4</font><font color=#006400>Subnet\u00A0-\u00A0</b></font></pre>"
				+ "<pre><font color=#000000><u><b>Michael</b></u></font>"
				+ "<font color=#c50000><u>Sheppard</u></font>"
				+ "<font color=#4169E1>\u00A0-\u00A0<b>2015</b></font></pre>\n";

		textView.append("\n");
		textView.append("--------------------------\n");

		textView.append(Html.fromHtml(logoString));
	}

    public void lineBreak(boolean line)
    {
        if (line) {
            textView.append("--------------------------\n");
        } else {
            textView.append("\n");
        }
    }

    public void displayResults()
    {
        textView.setText(""); // clear TextView

        format("Host Address", subnet.getIpAddr(), true);
        format("Host Address (hex)", subnet.getIpAddrHex(), true);
		format("Host Address (decimal)", subnet.getIpAddrDecimal(), true);
		format("Host Bits", Integer.toString(subnet.getHostBits()), true);
		format("Network Class", subnet.getNetworkClass(), true);
        format("Network Mask (bits)", Integer.toString(subnet.getNetworkBits()), true);
        format("Network Mask", subnet.getDecimalMaskOctets(), true);
        format("Binary Mask", subnet.getBinaryMaskOctets(), true);
        format("Cisco Wildcard", subnet.getWildcard(), true);
        lineBreak(true);
        format("Usable IPs", Integer.toString(subnet.getUsableHosts()), true);
        format("Total IPs", Integer.toString(subnet.getNumberOfAddresses()), true);
        lineBreak(true);

		if (subnet.getUsableHosts() > 1) {
			format("Network", subnet.getNetwork(), true);
			format("Broadcast", subnet.getBroadcast(), true);
			format("Available Networks", Integer.toString(subnet.getAvailableSubnets()), true);
			lineBreak(true);

			format("Network", subnet.getNetwork() + " - " + subnet.getBroadcast(), true);
			format("Usable", subnet.getMinHostAddr() + " - " + subnet.getMaxHostAddr(), true);

			if (subnet.getAvailableSubnets() <= MAX_RANGES) {
				lineBreak(true);
				formatText("Available Network Ranges -------", "#000000", true, true);
				String[] ranges = subnet.getRanges();
				if (ranges != null) {
					int count = 1;
					for (String range : ranges) {
						if (range != null && range.split(" - ")[0].equals(subnet.getNetwork())) {
							formatWithColors(Integer.toString(count) + ". Network", range, "#000000", "#006400", true);
						} else {
							formatWithColors(Integer.toString(count) + ". Network", range, "#000000", "#cc0000", true);
						}
						count++;
					}
				}
			} else {
				lineBreak(true);
				formatText("Available Network Ranges not shown, too many (" + subnet.getAvailableSubnets() + ")", "#a50000", true, true);
			}
		} else {
			formatWithColors("Host Address", subnet.getNetwork(), "#005500", "#0000ff", true);
		}

		displayLogo();

        // close the keyboard
		HideSoftKeyboard();
    }
}
