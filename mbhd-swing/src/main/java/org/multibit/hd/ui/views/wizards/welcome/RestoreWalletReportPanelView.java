package org.multibit.hd.ui.views.wizards.welcome;

import com.google.common.base.Optional;
import net.miginfocom.swing.MigLayout;
import org.joda.time.DateTime;
import org.multibit.hd.brit.seed_phrase.Bip39SeedPhraseGenerator;
import org.multibit.hd.brit.seed_phrase.SeedPhraseGenerator;
import org.multibit.hd.core.config.Configurations;
import org.multibit.hd.core.crypto.AESUtils;
import org.multibit.hd.core.dto.WalletId;
import org.multibit.hd.core.dto.WalletSummary;
import org.multibit.hd.core.managers.BackupManager;
import org.multibit.hd.core.managers.InstallationManager;
import org.multibit.hd.core.managers.WalletManager;
import org.multibit.hd.core.services.CoreServices;
import org.multibit.hd.core.utils.Dates;
import org.multibit.hd.ui.MultiBitUI;
import org.multibit.hd.ui.events.view.ViewEvents;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.views.components.Labels;
import org.multibit.hd.ui.views.components.Panels;
import org.multibit.hd.ui.views.components.enter_password.EnterPasswordModel;
import org.multibit.hd.ui.views.components.enter_seed_phrase.EnterSeedPhraseModel;
import org.multibit.hd.ui.views.components.panels.PanelDecorator;
import org.multibit.hd.ui.views.components.select_backup_summary.SelectBackupSummaryModel;
import org.multibit.hd.ui.views.fonts.AwesomeDecorator;
import org.multibit.hd.ui.views.fonts.AwesomeIcon;
import org.multibit.hd.ui.views.themes.Themes;
import org.multibit.hd.ui.views.wizards.AbstractWizard;
import org.multibit.hd.ui.views.wizards.AbstractWizardPanelView;
import org.multibit.hd.ui.views.wizards.WizardButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * <p>View to provide the following to UI:</p>
 * <ul>
 * <li>Show result of attempting to restore a wallet</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class RestoreWalletReportPanelView extends AbstractWizardPanelView<WelcomeWizardModel, String> {

  private static final Logger log = LoggerFactory.getLogger(RestoreWalletReportPanelView.class);

  // View
  private JLabel walletCreatedStatusLabel;

  /**
   * @param wizard    The wizard managing the states
   * @param panelName The panel name to filter events from components
   */
  public RestoreWalletReportPanelView(AbstractWizard<WelcomeWizardModel> wizard, String panelName) {

    super(wizard, panelName, MessageKey.RESTORE_WALLET_REPORT_TITLE, AwesomeIcon.FILE_TEXT);

  }

  @Override
  public void newPanelModel() {

    String model = "TODO replace with a proper model";
    setPanelModel(model);

    // No need to bind this to the wizard model

  }

  @Override
  public void initialiseContent(JPanel contentPanel) {

    contentPanel.setLayout(new MigLayout(
      Panels.migXYLayout(),
      "[][][]", // Column constraints
      "[]10[]10[]10[]" // Row constraints
    ));

    // Apply the theme
    contentPanel.setBackground(Themes.currentTheme.detailPanelBackground());

    // Initialise to failure
    walletCreatedStatusLabel = Labels.newWalletCreatedStatus(false);

    contentPanel.add(walletCreatedStatusLabel, "wrap");

  }

  @Override
  protected void initialiseButtons(AbstractWizard<WelcomeWizardModel> wizard) {

    PanelDecorator.addFinish(this, wizard);

  }

  @Override
  public void fireInitialStateViewEvents() {

    // Enable the finish button
    ViewEvents.fireWizardButtonEnabledEvent(getPanelName(), WizardButton.FINISH, true);

  }

  @Override
  public void updateFromComponentModels(Optional componentModel) {
    // Do nothing - panel model is updated via an action and wizard model is not applicable
  }

  @Override
  public boolean beforeShow() {

    boolean walletCreatedStatus = false;

    WelcomeWizardModel model = getWizardModel();

    log.debug("The select wallet choice is " + getWizardModel().getSelectWalletChoice());
    log.debug("The restore method is " + getWizardModel().getRestoreMethod());

    // There are two sorts of restore wallet method:
    // RESTORE_WALLET_SEED_PHRASE = restore from a seed phrase and timestamp
    // RESTORE_WALLET_BACKUP = restore from a seed phrase and wallet backup

    // Locate the installation directory
    File applicationDataDirectory = InstallationManager.getOrCreateApplicationDataDirectory();

    File cloudBackupLocation = null;
    if (Configurations.currentConfiguration != null) {
      String cloudBackupLocationString = Configurations.currentConfiguration.getApplication().getCloudBackupLocation();
      if (cloudBackupLocationString != null && !"".equals(cloudBackupLocationString)) {
        cloudBackupLocation = new File(cloudBackupLocationString);
      }
    }

    // Initialise backup (must be before Bitcoin network starts and on the main thread)
    BackupManager.INSTANCE.initialise(applicationDataDirectory, cloudBackupLocation);

    if (WelcomeWizardState.RESTORE_WALLET_SELECT_BACKUP.equals(getWizardModel().getRestoreMethod())) {
      log.debug("Performing a restore from a seed phrase and a wallet backup.");

      EnterSeedPhraseModel restoreWalletEnterSeedPhraseModel = model.getRestoreWalletEnterSeedPhraseModel();

      // Determine if the create wallet status is valid
      walletCreatedStatus = createWallet(restoreWalletEnterSeedPhraseModel.getSeedPhrase());

    } else if (WelcomeWizardState.RESTORE_WALLET_TIMESTAMP.equals(getWizardModel().getRestoreMethod())) {
      log.debug("Performing a restore from a seed phrase and a timestamp.");
      EnterSeedPhraseModel restoreEnterSeedPhraseModel = model.getRestoreWalletEnterSeedPhraseModel();
      EnterSeedPhraseModel restoreEnterTimestampModel = model.getRestoreWalletEnterTimestampModel();
      EnterPasswordModel enterPasswordModel = model.getRestoreWalletEnterPasswordModel();
      log.debug("Timestamp = " + restoreEnterTimestampModel.getSeedTimestamp());

      walletCreatedStatus = createWallet(restoreEnterSeedPhraseModel.getSeedPhrase(), restoreEnterTimestampModel.getSeedTimestamp(), enterPasswordModel.getValue());
    } else {
      log.error("Cannot perform a restore - unknown method of restore = '" + getWizardModel().getRestoreMethod() + "'.");
    }

    if (walletCreatedStatus) {
      AwesomeDecorator.applyIcon(AwesomeIcon.CHECK, walletCreatedStatusLabel, true, MultiBitUI.NORMAL_ICON_SIZE);
    } else {
      AwesomeDecorator.applyIcon(AwesomeIcon.TIMES, walletCreatedStatusLabel, true, MultiBitUI.NORMAL_ICON_SIZE);
    }

    ViewEvents.fireWizardButtonEnabledEvent(WelcomeWizardState.CREATE_WALLET_REPORT.name(), WizardButton.FINISH, true);

    return true;
  }

  @Override
  public void afterShow() {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        getFinishButton().requestFocusInWindow();
      }
    });

  }


  /**
   * Create a wallet from a seed phrase, timestamp and password
   */
  private boolean createWallet(List<String> seedPhrase, String timestamp, CharSequence password) {

    if (!checkSeedPhrase(seedPhrase)) return false;

    SeedPhraseGenerator seedGenerator = new Bip39SeedPhraseGenerator();
    byte[] seed = seedGenerator.convertToSeed(seedPhrase);

    try {
      // Locate the user data directory
      File applicationDataDirectory = InstallationManager.getOrCreateApplicationDataDirectory();

      DateTime replayDate = Dates.parseSeedTimestamp(timestamp);
      // TODO necessary to backup any existing wallet with the same seed before creation/ overwrite ?
      WalletManager.INSTANCE.createWalletSummary(seed, (long) (replayDate.getMillis() * 0.001), password);

      // Initialise the WalletService with the newly created wallet, which provides transaction information from the wallet
      Optional<WalletSummary> currentWalletSummary = WalletManager.INSTANCE.getCurrentWalletSummary();

      WalletManager.writeEncryptedPasswordAndBackupKey(currentWalletSummary.get(), seed, (String)password);

      String walletRoot = WalletManager.createWalletRoot(currentWalletSummary.get().getWalletId());
      File walletDirectory = WalletManager.getOrCreateWalletDirectory(applicationDataDirectory, walletRoot);

      File walletSummaryFile = WalletManager.getOrCreateWalletSummaryFile(walletDirectory);
      WalletManager.updateWalletSummary(walletSummaryFile, currentWalletSummary.get());

      if (currentWalletSummary.isPresent()) {
        // Create a wallet service
        CoreServices.getOrCreateWalletService(currentWalletSummary.get().getWalletId());

        // Start the Bitcoin network to synchronize
        CoreServices.getOrCreateBitcoinNetworkService().replayWallet(replayDate);

        return true;
      }

    } catch (Exception e) {
      e.printStackTrace();
      log.error("Failed to restore wallet. Error was '" + e.getMessage() + "'.");
    }

    // Must have failed to be here
    return false;

  }

  /**
   * Create a wallet from a seed phrase and a backup summary (chosen by the user)
   */
  private boolean createWallet(List<String> seedPhrase) {

    if (!checkSeedPhrase(seedPhrase)) return false;

    // Get the model that contains the selected wallet backup to use
    SelectBackupSummaryModel selectedBackupSummaryModel = getWizardModel().getSelectBackupSummaryModel();

    if (selectedBackupSummaryModel == null || selectedBackupSummaryModel.getValue() == null ||
      selectedBackupSummaryModel.getValue().getFile() == null) {
      log.debug("No wallet backup to loadContacts from the model");
      return false;
    }

    log.debug("Loading wallet backup '" + selectedBackupSummaryModel.getValue().getFile() + "'");
    try {

      WalletId loadedWalletId = BackupManager.INSTANCE.loadBackup(selectedBackupSummaryModel.getValue().getFile(), seedPhrase);

      // Locate the installation directory
      File applicationDataDirectory = InstallationManager.getOrCreateApplicationDataDirectory();

      // Work out what the wallet password was from the encrypted value stored in the WalletSummary
      SeedPhraseGenerator seedPhraseGenerator = new Bip39SeedPhraseGenerator();
      byte[] seed = seedPhraseGenerator.convertToSeed(seedPhrase);

      String walletRoot = applicationDataDirectory.getAbsolutePath() + File.separator + WalletManager.createWalletRoot(loadedWalletId);
      WalletSummary walletSummary = WalletManager.getOrCreateWalletSummary(new File(walletRoot), loadedWalletId);

      KeyParameter backupAESkey = AESUtils.createAESKey(seed, WalletManager.SCRYPT_SALT);
      byte[] decryptedWalletPasswordBytes = org.multibit.hd.brit.crypto.AESUtils.decrypt(walletSummary.getEncryptedPassword(), backupAESkey, WalletManager.AES_INITIALISATION_VECTOR);
      String decryptedWalletPassword = new String(decryptedWalletPasswordBytes, "UTF8");

      // TODO need to shut down everything beforehand ???
      WalletManager.INSTANCE.open(
        InstallationManager.getOrCreateApplicationDataDirectory(),
        loadedWalletId,
        decryptedWalletPassword);

      // Synchronize wallet
      CoreServices.getOrCreateBitcoinNetworkService().start();

      return true;
    } catch (Exception e) {
      log.error("Failed to restore wallet. Error was '" + e.getMessage() + "'.");
      return false;
    }
  }

  private boolean checkSeedPhrase(List<String> seedPhrase) {

    if (seedPhrase == null || seedPhrase.size() == 0) {
      log.error("No seed phrase specified. Cannot restore wallet.");
      return false;
    }

    return true;
  }
}
