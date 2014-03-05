package org.multibit.hd.ui.views.components.renderers;

import org.joda.money.BigMoney;
import org.multibit.hd.core.config.BitcoinConfiguration;
import org.multibit.hd.core.config.Configurations;
import org.multibit.hd.core.config.I18NConfiguration;
import org.multibit.hd.core.dto.FiatPayment;
import org.multibit.hd.ui.i18n.Formats;
import org.multibit.hd.ui.views.components.Labels;
import org.multibit.hd.ui.views.components.tables.StripedTable;
import org.multibit.hd.ui.views.themes.Themes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigInteger;

/**
 *  <p>Renderer to provide the following to tables:<br>
 *  <ul>
 *  <li>Renderer of numeric amount field</li>
 *  </ul>
 *  
 */
public class AmountFiatTableCellRenderer extends DefaultTableCellRenderer {
  JLabel label;

  public AmountFiatTableCellRenderer() {
    label = Labels.newBlankLabel();
  }

  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                                                 int column) {
    label.setHorizontalAlignment(SwingConstants.TRAILING);
    label.setOpaque(true);
    label.setBorder(new EmptyBorder(new Insets(0, TrailingJustifiedDateTableCellRenderer.TABLE_BORDER, 1, TrailingJustifiedDateTableCellRenderer.TABLE_BORDER)));

    if (value instanceof BigInteger) {

      // Do the Bitcoin processing

      FiatPayment fiatPayment = (FiatPayment) value;
      Double amountAsDouble = Double.parseDouble(fiatPayment.getAmount());
      I18NConfiguration i18nConfiguration = Configurations.currentConfiguration.getI18NConfiguration();
      BitcoinConfiguration bitcoinConfiguration = Configurations.currentConfiguration.getBitcoinConfiguration();

      String balance = Formats.formatLocalAmount(BigMoney.of(bitcoinConfiguration.getLocalCurrencyUnit(), amountAsDouble), i18nConfiguration.getLocale(), bitcoinConfiguration);

      label.setText(balance+ TrailingJustifiedDateTableCellRenderer.SPACER);

      if (amountAsDouble < 0) {
        // Debit
        if (isSelected) {
          label.setForeground(table.getSelectionForeground());
        } else {
          label.setForeground(Themes.currentTheme.debitText());
        }
      } else {
        // Credit
        if (isSelected) {
          label.setForeground(table.getSelectionForeground());
        } else {
          label.setForeground(Themes.currentTheme.creditText());
        }
      }
      if (isSelected) {
        label.setBackground(table.getSelectionBackground());
        label.setForeground(table.getSelectionForeground());
      } else {
        if (row % 2 == 1) {
          label.setBackground(StripedTable.alternateColor);
        } else {
          label.setBackground(StripedTable.rowColor);
        }
      }
    }

    return label;
  }
}
