@echo off
setlocal EnableExtensions
set "LL_INSTALLER_PATH=%~f0"
set "LL_POWERSHELL=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if defined PROCESSOR_ARCHITEW6432 if exist "%SystemRoot%\Sysnative\WindowsPowerShell\v1.0\powershell.exe" set "LL_POWERSHELL=%SystemRoot%\Sysnative\WindowsPowerShell\v1.0\powershell.exe"
if exist "%LL_POWERSHELL%" goto ll_run
where powershell.exe >nul 2>nul
if errorlevel 1 (
    echo.
    echo LiteLauncher Installer
    echo ERROR: Windows PowerShell is not available.
    echo Install or enable Windows PowerShell and run this installer again.
    echo.
    pause
    exit /b 1
)
set "LL_POWERSHELL=powershell.exe"
:ll_run
"%LL_POWERSHELL%" -NoLogo -NoProfile -Command "try { $installer=$env:LL_INSTALLER_PATH; $text=[IO.File]::ReadAllText($installer); $marker='#'+'<LITELAUNCHER_POWERSHELL>'; $offset=$text.LastIndexOf($marker,[StringComparison]::Ordinal); if($offset -lt 0){throw 'Embedded installer script was not found.'}; $script=$text.Substring($offset+$marker.Length); & ([ScriptBlock]::Create($script)) } catch { Write-Host ''; Write-Host 'LiteLauncher Installer' -ForegroundColor Cyan; Write-Host ('ERROR: ' + $_.Exception.Message) -ForegroundColor Red; exit 20 }"
set "LL_EXIT=%ERRORLEVEL%"
if "%LL_EXIT%"=="10" (
    endlocal & exit /b 1
)
if not "%LL_EXIT%"=="0" (
    echo.
    echo Installer could not start correctly. See the error above.
    pause
)
endlocal & exit /b %LL_EXIT%
#<LITELAUNCHER_POWERSHELL>

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

$script:RuntimeId = 'jre-25'
$script:InstallerBuild = 'unified-installers-v12.2-20260813'
$script:ManifestUrl = 'https://litelauncher.net/api/v1/launcher/java_manifest.json'
$script:Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$script:LogPath = $null
$script:ProgressActive = $false
$script:LastProgressLength = 0

$script:Translations = @{
    'en' = @{
        ActionFailed = 'The selected action could not be completed.'
        AlreadyInstalled = 'LiteLauncher is already installed.'
        CheckingFound = 'found — existing Java will be kept'
        CheckingJava = 'Checking installed Java...'
        CheckingNotFound = 'not found'
        CreateLocalShortcut = 'Create shortcut in this folder'
        CreateLocalShortcutDesc = 'Creates or replaces a LiteLauncher shortcut in the folder containing this installer. LiteLauncher will not be reinstalled.'
        CreatingShortcut = 'Creating shortcut...'
        Done = 'Completed.'
        DownloadingJava = 'Downloading Java...'
        ErrorLabel = 'Error: '
        ExitDesc = 'Closes the installer without making changes.'
        ExitInstaller = 'Exit installer'
        InstallationFailed = 'LiteLauncher could not be installed.'
        InstalledSuccessfully = 'LiteLauncher has been installed successfully.'
        InstallingJava = 'Installing Java...'
        InteractiveRequired = 'User input is required because LiteLauncher is already installed.'
        JavaReady = 'Java is ready.'
        LoadingJava = 'Retrieving Java information...'
        LogLabel = 'Log:   '
        NoChanges = 'No changes were made.'
        OpenLauncher = 'Open LiteLauncher'
        OpenLauncherDesc = 'Starts LiteLauncher.'
        OpenShortcutFolder = 'Open shortcut folder'
        OpenShortcutFolderDesc = 'Opens the folder containing the installed LiteLauncher shortcut.'
        OpeningShortcutFolder = 'Opening shortcut folder...'
        PostInstallPrompt = 'Select the next action:'
        PostOpenFolderDesc = 'Opens the folder containing the installed LiteLauncher shortcut.'
        PreparingFiles = 'Preparing installation files...'
        PreparingJava = 'Preparing Java...'
        PressLog = 'Press L to open the log, or any other key to close the installer.'
        PressMain = 'Select an option (1-5): '
        PressPost = 'Select an option (1-3): '
        Reinstall = 'Reinstall LiteLauncher'
        ReinstallDesc = 'Removes the current installation, including Java 25 installed with LiteLauncher, and performs a fresh installation.'
        RemovingLauncher = 'Removing LiteLauncher...'
        RemovingShortcut = 'Removing shortcut...'
        ShortcutCreatedSuccessfully = 'LiteLauncher shortcut created successfully.'
        ShortcutLabel = 'Shortcut: '
        StartingLauncher = 'Starting LiteLauncher...'
        Uninstall = 'Uninstall LiteLauncher'
        UninstallDesc = 'Removes LiteLauncher, Java 25 installed with it, and the installed shortcut.'
        UninstalledSuccessfully = 'LiteLauncher has been uninstalled successfully.'
        VerifyingJava = 'verifying'
        WritingBootstrap = 'Installing core components...'
        WritingIcons = 'Installing icons...'
        WritingLauncher = 'Installing launcher files...'
    }
    'es' = @{
        ActionFailed = 'No se pudo completar la acción seleccionada.'
        AlreadyInstalled = 'LiteLauncher ya está instalado.'
        CheckingFound = 'encontrada — se conservará la instalación existente'
        CheckingJava = 'Comprobando la instalación de Java...'
        CheckingNotFound = 'no encontrada'
        CreateLocalShortcut = 'Crear acceso directo en esta carpeta'
        CreateLocalShortcutDesc = 'Crea o reemplaza un acceso directo de LiteLauncher en la carpeta que contiene este instalador. LiteLauncher no se reinstalará.'
        CreatingShortcut = 'Creando acceso directo...'
        Done = 'Completado.'
        DownloadingJava = 'Descargando Java...'
        ErrorLabel = 'Error: '
        ExitDesc = 'Cierra el instalador sin realizar cambios.'
        ExitInstaller = 'Salir del instalador'
        InstallationFailed = 'No se pudo instalar LiteLauncher.'
        InstalledSuccessfully = 'LiteLauncher se ha instalado correctamente.'
        InstallingJava = 'Instalando Java...'
        InteractiveRequired = 'Debe seleccionar una acción porque LiteLauncher ya está instalado.'
        JavaReady = 'Java está listo.'
        LoadingJava = 'Obteniendo información de Java...'
        LogLabel = 'Registro: '
        NoChanges = 'No se realizaron cambios.'
        OpenLauncher = 'Abrir LiteLauncher'
        OpenLauncherDesc = 'Inicia LiteLauncher.'
        OpenShortcutFolder = 'Abrir carpeta del acceso directo'
        OpenShortcutFolderDesc = 'Abre la carpeta que contiene el acceso directo instalado de LiteLauncher.'
        OpeningShortcutFolder = 'Abriendo la carpeta del acceso directo...'
        PostInstallPrompt = 'Seleccione la siguiente acción:'
        PostOpenFolderDesc = 'Abre la carpeta que contiene el acceso directo instalado de LiteLauncher.'
        PreparingFiles = 'Preparando los archivos de instalación...'
        PreparingJava = 'Preparando Java...'
        PressLog = 'Pulse L para abrir el registro o cualquier otra tecla para cerrar el instalador.'
        PressMain = 'Seleccione una opción (1-5): '
        PressPost = 'Seleccione una opción (1-3): '
        Reinstall = 'Reinstalar LiteLauncher'
        ReinstallDesc = 'Elimina la instalación actual, incluida la versión de Java 25 instalada con LiteLauncher, y realiza una instalación nueva.'
        RemovingLauncher = 'Eliminando LiteLauncher...'
        RemovingShortcut = 'Eliminando acceso directo...'
        ShortcutCreatedSuccessfully = 'El acceso directo de LiteLauncher se ha creado correctamente.'
        ShortcutLabel = 'Acceso directo: '
        StartingLauncher = 'Iniciando LiteLauncher...'
        Uninstall = 'Desinstalar LiteLauncher'
        UninstallDesc = 'Elimina LiteLauncher, la versión de Java 25 instalada con él y el acceso directo instalado.'
        UninstalledSuccessfully = 'LiteLauncher se ha desinstalado correctamente.'
        VerifyingJava = 'verificando'
        WritingBootstrap = 'Instalando componentes principales...'
        WritingIcons = 'Instalando iconos...'
        WritingLauncher = 'Instalando archivos de inicio...'
    }
    'ru' = @{
        ActionFailed = 'Не удалось выполнить выбранное действие.'
        AlreadyInstalled = 'LiteLauncher уже установлен.'
        CheckingFound = 'найдена — установленная Java будет сохранена'
        CheckingJava = 'Проверка установленной Java...'
        CheckingNotFound = 'не найдена'
        CreateLocalShortcut = 'Создать ярлык в этой папке'
        CreateLocalShortcutDesc = 'Создаёт или заменяет ярлык LiteLauncher в папке с этим установщиком. Переустановка LiteLauncher не выполняется.'
        CreatingShortcut = 'Создание ярлыка...'
        Done = 'Готово.'
        DownloadingJava = 'Загрузка Java...'
        ErrorLabel = 'Ошибка: '
        ExitDesc = 'Закрывает установщик без внесения изменений.'
        ExitInstaller = 'Выйти из установщика'
        InstallationFailed = 'Не удалось установить LiteLauncher.'
        InstalledSuccessfully = 'LiteLauncher успешно установлен.'
        InstallingJava = 'Установка Java...'
        InteractiveRequired = 'Для продолжения требуется выбрать действие, поскольку LiteLauncher уже установлен.'
        JavaReady = 'Java готова.'
        LoadingJava = 'Получение информации о Java...'
        LogLabel = 'Лог:    '
        NoChanges = 'Изменения не вносились.'
        OpenLauncher = 'Открыть LiteLauncher'
        OpenLauncherDesc = 'Запускает LiteLauncher.'
        OpenShortcutFolder = 'Открыть папку с ярлыком'
        OpenShortcutFolderDesc = 'Открывает папку, содержащую установленный ярлык LiteLauncher.'
        OpeningShortcutFolder = 'Открытие папки с ярлыком...'
        PostInstallPrompt = 'Выберите дальнейшее действие:'
        PostOpenFolderDesc = 'Открывает папку, содержащую установленный ярлык LiteLauncher.'
        PreparingFiles = 'Подготовка файлов установки...'
        PreparingJava = 'Подготовка Java...'
        PressLog = 'Нажмите L, чтобы открыть лог, или любую другую клавишу, чтобы закрыть установщик.'
        PressMain = 'Выберите вариант (1-5): '
        PressPost = 'Выберите вариант (1-3): '
        Reinstall = 'Переустановить LiteLauncher'
        ReinstallDesc = 'Удаляет текущую установку, включая Java 25, установленную вместе с LiteLauncher, и выполняет чистую переустановку.'
        RemovingLauncher = 'Удаление LiteLauncher...'
        RemovingShortcut = 'Удаление ярлыка...'
        ShortcutCreatedSuccessfully = 'Ярлык LiteLauncher успешно создан.'
        ShortcutLabel = 'Ярлык: '
        StartingLauncher = 'Запуск LiteLauncher...'
        Uninstall = 'Удалить LiteLauncher'
        UninstallDesc = 'Удаляет LiteLauncher, Java 25, установленную вместе с ним, и установленный ярлык.'
        UninstalledSuccessfully = 'LiteLauncher успешно удалён.'
        VerifyingJava = 'проверка'
        WritingBootstrap = 'Установка основных компонентов...'
        WritingIcons = 'Установка значков...'
        WritingLauncher = 'Установка файлов запуска...'
    }
    'pt' = @{
        ActionFailed = 'Não foi possível concluir a ação selecionada.'
        AlreadyInstalled = 'LiteLauncher já está instalado.'
        CheckingFound = 'encontrada — a instalação existente será mantida'
        CheckingJava = 'Verificando a instalação do Java...'
        CheckingNotFound = 'não encontrada'
        CreateLocalShortcut = 'Criar atalho nesta pasta'
        CreateLocalShortcutDesc = 'Cria ou substitui um atalho do LiteLauncher na pasta deste instalador. O LiteLauncher não será reinstalado.'
        CreatingShortcut = 'Criando atalho...'
        Done = 'Concluído.'
        DownloadingJava = 'Baixando Java...'
        ErrorLabel = 'Erro: '
        ExitDesc = 'Fecha o instalador sem fazer alterações.'
        ExitInstaller = 'Sair do instalador'
        InstallationFailed = 'Não foi possível instalar o LiteLauncher.'
        InstalledSuccessfully = 'LiteLauncher foi instalado com sucesso.'
        InstallingJava = 'Instalando Java...'
        InteractiveRequired = 'É necessário selecionar uma ação porque o LiteLauncher já está instalado.'
        JavaReady = 'Java está pronto.'
        LoadingJava = 'Obtendo informações do Java...'
        LogLabel = 'Log:   '
        NoChanges = 'Nenhuma alteração foi feita.'
        OpenLauncher = 'Abrir LiteLauncher'
        OpenLauncherDesc = 'Inicia o LiteLauncher.'
        OpenShortcutFolder = 'Abrir pasta do atalho'
        OpenShortcutFolderDesc = 'Abre a pasta que contém o atalho instalado do LiteLauncher.'
        OpeningShortcutFolder = 'Abrindo a pasta do atalho...'
        PostInstallPrompt = 'Selecione a próxima ação:'
        PostOpenFolderDesc = 'Abre a pasta que contém o atalho instalado do LiteLauncher.'
        PreparingFiles = 'Preparando os arquivos de instalação...'
        PreparingJava = 'Preparando Java...'
        PressLog = 'Pressione L para abrir o log ou qualquer outra tecla para fechar o instalador.'
        PressMain = 'Selecione uma opção (1-5): '
        PressPost = 'Selecione uma opção (1-3): '
        Reinstall = 'Reinstalar LiteLauncher'
        ReinstallDesc = 'Remove a instalação atual, incluindo o Java 25 instalado com o LiteLauncher, e realiza uma nova instalação.'
        RemovingLauncher = 'Removendo LiteLauncher...'
        RemovingShortcut = 'Removendo atalho...'
        ShortcutCreatedSuccessfully = 'O atalho do LiteLauncher foi criado com sucesso.'
        ShortcutLabel = 'Atalho: '
        StartingLauncher = 'Iniciando LiteLauncher...'
        Uninstall = 'Desinstalar LiteLauncher'
        UninstallDesc = 'Remove o LiteLauncher, o Java 25 instalado com ele e o atalho instalado.'
        UninstalledSuccessfully = 'LiteLauncher foi desinstalado com sucesso.'
        VerifyingJava = 'verificando'
        WritingBootstrap = 'Instalando componentes principais...'
        WritingIcons = 'Instalando ícones...'
        WritingLauncher = 'Instalando arquivos de inicialização...'
    }
    'de' = @{
        ActionFailed = 'Die ausgewählte Aktion konnte nicht abgeschlossen werden.'
        AlreadyInstalled = 'LiteLauncher ist bereits installiert.'
        CheckingFound = 'gefunden — vorhandenes Java wird beibehalten'
        CheckingJava = 'Installiertes Java wird geprüft...'
        CheckingNotFound = 'nicht gefunden'
        CreateLocalShortcut = 'Verknüpfung in diesem Ordner erstellen'
        CreateLocalShortcutDesc = 'Erstellt oder ersetzt eine LiteLauncher-Verknüpfung im Ordner dieses Installers. LiteLauncher wird nicht neu installiert.'
        CreatingShortcut = 'Verknüpfung wird erstellt...'
        Done = 'Abgeschlossen.'
        DownloadingJava = 'Java wird heruntergeladen...'
        ErrorLabel = 'Fehler: '
        ExitDesc = 'Schließt den Installer, ohne Änderungen vorzunehmen.'
        ExitInstaller = 'Installer beenden'
        InstallationFailed = 'LiteLauncher konnte nicht installiert werden.'
        InstalledSuccessfully = 'LiteLauncher wurde erfolgreich installiert.'
        InstallingJava = 'Java wird installiert...'
        InteractiveRequired = 'Da LiteLauncher bereits installiert ist, muss eine Aktion ausgewählt werden.'
        JavaReady = 'Java ist bereit.'
        LoadingJava = 'Java-Informationen werden abgerufen...'
        LogLabel = 'Log:    '
        NoChanges = 'Es wurden keine Änderungen vorgenommen.'
        OpenLauncher = 'LiteLauncher öffnen'
        OpenLauncherDesc = 'Startet LiteLauncher.'
        OpenShortcutFolder = 'Ordner mit Verknüpfung öffnen'
        OpenShortcutFolderDesc = 'Öffnet den Ordner mit der installierten LiteLauncher-Verknüpfung.'
        OpeningShortcutFolder = 'Ordner mit Verknüpfung wird geöffnet...'
        PostInstallPrompt = 'Wählen Sie die nächste Aktion:'
        PostOpenFolderDesc = 'Öffnet den Ordner mit der installierten LiteLauncher-Verknüpfung.'
        PreparingFiles = 'Installationsdateien werden vorbereitet...'
        PreparingJava = 'Java wird vorbereitet...'
        PressLog = 'Drücken Sie L, um das Protokoll zu öffnen, oder eine andere Taste, um den Installer zu schließen.'
        PressMain = 'Wählen Sie eine Option (1-5): '
        PressPost = 'Wählen Sie eine Option (1-3): '
        Reinstall = 'LiteLauncher neu installieren'
        ReinstallDesc = 'Entfernt die aktuelle Installation einschließlich des mit LiteLauncher installierten Java 25 und führt eine Neuinstallation durch.'
        RemovingLauncher = 'LiteLauncher wird entfernt...'
        RemovingShortcut = 'Verknüpfung wird entfernt...'
        ShortcutCreatedSuccessfully = 'Die LiteLauncher-Verknüpfung wurde erfolgreich erstellt.'
        ShortcutLabel = 'Verknüpfung: '
        StartingLauncher = 'LiteLauncher wird gestartet...'
        Uninstall = 'LiteLauncher deinstallieren'
        UninstallDesc = 'Entfernt LiteLauncher, das mitinstallierte Java 25 und die installierte Verknüpfung.'
        UninstalledSuccessfully = 'LiteLauncher wurde erfolgreich deinstalliert.'
        VerifyingJava = 'wird überprüft'
        WritingBootstrap = 'Kernkomponenten werden installiert...'
        WritingIcons = 'Symbole werden installiert...'
        WritingLauncher = 'Launcher-Dateien werden installiert...'
    }
    'fr' = @{
        ActionFailed = 'L''action sélectionnée n''a pas pu être effectuée.'
        AlreadyInstalled = 'LiteLauncher est déjà installé.'
        CheckingFound = 'trouvée — l''installation existante sera conservée'
        CheckingJava = 'Vérification de l''installation Java...'
        CheckingNotFound = 'introuvable'
        CreateLocalShortcut = 'Créer un raccourci dans ce dossier'
        CreateLocalShortcutDesc = 'Crée ou remplace un raccourci LiteLauncher dans le dossier contenant cet installateur. LiteLauncher ne sera pas réinstallé.'
        CreatingShortcut = 'Création du raccourci...'
        Done = 'Terminé.'
        DownloadingJava = 'Téléchargement de Java...'
        ErrorLabel = 'Erreur : '
        ExitDesc = 'Ferme l''installateur sans apporter de modification.'
        ExitInstaller = 'Quitter l''installateur'
        InstallationFailed = 'LiteLauncher n''a pas pu être installé.'
        InstalledSuccessfully = 'LiteLauncher a été installé avec succès.'
        InstallingJava = 'Installation de Java...'
        InteractiveRequired = 'Vous devez sélectionner une action, car LiteLauncher est déjà installé.'
        JavaReady = 'Java est prêt.'
        LoadingJava = 'Récupération des informations Java...'
        LogLabel = 'Journal : '
        NoChanges = 'Aucune modification n''a été effectuée.'
        OpenLauncher = 'Ouvrir LiteLauncher'
        OpenLauncherDesc = 'Démarre LiteLauncher.'
        OpenShortcutFolder = 'Ouvrir le dossier du raccourci'
        OpenShortcutFolderDesc = 'Ouvre le dossier contenant le raccourci LiteLauncher installé.'
        OpeningShortcutFolder = 'Ouverture du dossier du raccourci...'
        PostInstallPrompt = 'Sélectionnez l''action suivante :'
        PostOpenFolderDesc = 'Ouvre le dossier contenant le raccourci LiteLauncher installé.'
        PreparingFiles = 'Préparation des fichiers d''installation...'
        PreparingJava = 'Préparation de Java...'
        PressLog = 'Appuyez sur L pour ouvrir le journal, ou sur une autre touche pour fermer l''installateur.'
        PressMain = 'Sélectionnez une option (1-5) : '
        PressPost = 'Sélectionnez une option (1-3) : '
        Reinstall = 'Réinstaller LiteLauncher'
        ReinstallDesc = 'Supprime l''installation actuelle, y compris Java 25 installé avec LiteLauncher, puis effectue une nouvelle installation.'
        RemovingLauncher = 'Suppression de LiteLauncher...'
        RemovingShortcut = 'Suppression du raccourci...'
        ShortcutCreatedSuccessfully = 'Le raccourci LiteLauncher a été créé avec succès.'
        ShortcutLabel = 'Raccourci : '
        StartingLauncher = 'Démarrage de LiteLauncher...'
        Uninstall = 'Désinstaller LiteLauncher'
        UninstallDesc = 'Supprime LiteLauncher, la version de Java 25 installée avec LiteLauncher et le raccourci installé.'
        UninstalledSuccessfully = 'LiteLauncher a été désinstallé avec succès.'
        VerifyingJava = 'vérification'
        WritingBootstrap = 'Installation des composants principaux...'
        WritingIcons = 'Installation des icônes...'
        WritingLauncher = 'Installation des fichiers de lancement...'
    }
    'tr' = @{
        ActionFailed = 'Seçilen işlem tamamlanamadı.'
        AlreadyInstalled = 'LiteLauncher zaten yüklü.'
        CheckingFound = 'bulundu — mevcut Java kurulumu korunacak'
        CheckingJava = 'Yüklü Java kontrol ediliyor...'
        CheckingNotFound = 'bulunamadı'
        CreateLocalShortcut = 'Bu klasörde kısayol oluştur'
        CreateLocalShortcutDesc = 'Bu yükleyicinin bulunduğu klasörde LiteLauncher kısayolu oluşturur veya mevcut kısayolu değiştirir. LiteLauncher yeniden yüklenmez.'
        CreatingShortcut = 'Kısayol oluşturuluyor...'
        Done = 'Tamamlandı.'
        DownloadingJava = 'Java indiriliyor...'
        ErrorLabel = 'Hata: '
        ExitDesc = 'Değişiklik yapmadan yükleyiciyi kapatır.'
        ExitInstaller = 'Yükleyiciden çık'
        InstallationFailed = 'LiteLauncher yüklenemedi.'
        InstalledSuccessfully = 'LiteLauncher başarıyla yüklendi.'
        InstallingJava = 'Java kuruluyor...'
        InteractiveRequired = 'LiteLauncher zaten yüklü olduğundan bir işlem seçmeniz gerekiyor.'
        JavaReady = 'Java hazır.'
        LoadingJava = 'Java bilgileri alınıyor...'
        LogLabel = 'Günlük: '
        NoChanges = 'Hiçbir değişiklik yapılmadı.'
        OpenLauncher = 'LiteLauncher''ı aç'
        OpenLauncherDesc = 'LiteLauncher''ı başlatır.'
        OpenShortcutFolder = 'Kısayol klasörünü aç'
        OpenShortcutFolderDesc = 'Yüklü LiteLauncher kısayolunu içeren klasörü açar.'
        OpeningShortcutFolder = 'Kısayol klasörü açılıyor...'
        PostInstallPrompt = 'Sonraki işlemi seçin:'
        PostOpenFolderDesc = 'Yüklü LiteLauncher kısayolunu içeren klasörü açar.'
        PreparingFiles = 'Kurulum dosyaları hazırlanıyor...'
        PreparingJava = 'Java hazırlanıyor...'
        PressLog = 'Günlüğü açmak için L tuşuna, yükleyiciyi kapatmak için başka bir tuşa basın.'
        PressMain = 'Bir seçenek belirleyin (1-5): '
        PressPost = 'Bir seçenek belirleyin (1-3): '
        Reinstall = 'LiteLauncher''ı yeniden yükle'
        ReinstallDesc = 'LiteLauncher ile yüklenen Java 25 dahil mevcut kurulumu kaldırır ve temiz bir kurulum yapar.'
        RemovingLauncher = 'LiteLauncher kaldırılıyor...'
        RemovingShortcut = 'Kısayol kaldırılıyor...'
        ShortcutCreatedSuccessfully = 'LiteLauncher kısayolu başarıyla oluşturuldu.'
        ShortcutLabel = 'Kısayol: '
        StartingLauncher = 'LiteLauncher başlatılıyor...'
        Uninstall = 'LiteLauncher''ı kaldır'
        UninstallDesc = 'LiteLauncher''ı, onunla birlikte yüklenen Java 25''i ve yüklü kısayolu kaldırır.'
        UninstalledSuccessfully = 'LiteLauncher başarıyla kaldırıldı.'
        VerifyingJava = 'doğrulanıyor'
        WritingBootstrap = 'Temel bileşenler yükleniyor...'
        WritingIcons = 'Simgeler yükleniyor...'
        WritingLauncher = 'Başlatma dosyaları yükleniyor...'
    }
    'pl' = @{
        ActionFailed = 'Nie udało się wykonać wybranej operacji.'
        AlreadyInstalled = 'LiteLauncher jest już zainstalowany.'
        CheckingFound = 'znaleziona — istniejąca instalacja Javy zostanie zachowana'
        CheckingJava = 'Sprawdzanie zainstalowanej Javy...'
        CheckingNotFound = 'nie znaleziono'
        CreateLocalShortcut = 'Utwórz skrót w tym folderze'
        CreateLocalShortcutDesc = 'Tworzy lub zastępuje skrót LiteLauncher w folderze tego instalatora. LiteLauncher nie zostanie ponownie zainstalowany.'
        CreatingShortcut = 'Tworzenie skrótu...'
        Done = 'Gotowe.'
        DownloadingJava = 'Pobieranie Javy...'
        ErrorLabel = 'Błąd: '
        ExitDesc = 'Zamyka instalator bez wprowadzania zmian.'
        ExitInstaller = 'Zamknij instalator'
        InstallationFailed = 'Nie udało się zainstalować LiteLauncher.'
        InstalledSuccessfully = 'LiteLauncher został pomyślnie zainstalowany.'
        InstallingJava = 'Instalowanie Javy...'
        InteractiveRequired = 'Ponieważ LiteLauncher jest już zainstalowany, należy wybrać działanie.'
        JavaReady = 'Java jest gotowa.'
        LoadingJava = 'Odczytywanie informacji o Javie...'
        LogLabel = 'Log:   '
        NoChanges = 'Nie wprowadzono żadnych zmian.'
        OpenLauncher = 'Otwórz LiteLauncher'
        OpenLauncherDesc = 'Uruchamia LiteLauncher.'
        OpenShortcutFolder = 'Otwórz folder skrótu'
        OpenShortcutFolderDesc = 'Otwiera folder zawierający zainstalowany skrót LiteLauncher.'
        OpeningShortcutFolder = 'Otwieranie folderu skrótu...'
        PostInstallPrompt = 'Wybierz dalsze działanie:'
        PostOpenFolderDesc = 'Otwiera folder zawierający zainstalowany skrót LiteLauncher.'
        PreparingFiles = 'Przygotowywanie plików instalacyjnych...'
        PreparingJava = 'Przygotowywanie Javy...'
        PressLog = 'Naciśnij L, aby otworzyć log, lub dowolny inny klawisz, aby zamknąć instalator.'
        PressMain = 'Wybierz opcję (1-5): '
        PressPost = 'Wybierz opcję (1-3): '
        Reinstall = 'Zainstaluj LiteLauncher ponownie'
        ReinstallDesc = 'Usuwa bieżącą instalację, w tym Javę 25 zainstalowaną wraz z LiteLauncher, a następnie wykonuje nową instalację.'
        RemovingLauncher = 'Usuwanie LiteLauncher...'
        RemovingShortcut = 'Usuwanie skrótu...'
        ShortcutCreatedSuccessfully = 'Skrót LiteLauncher został pomyślnie utworzony.'
        ShortcutLabel = 'Skrót: '
        StartingLauncher = 'Uruchamianie LiteLauncher...'
        Uninstall = 'Odinstaluj LiteLauncher'
        UninstallDesc = 'Usuwa LiteLauncher, Javę 25 zainstalowaną wraz z nim oraz zainstalowany skrót.'
        UninstalledSuccessfully = 'LiteLauncher został pomyślnie odinstalowany.'
        VerifyingJava = 'weryfikacja'
        WritingBootstrap = 'Instalowanie głównych składników...'
        WritingIcons = 'Instalowanie ikon...'
        WritingLauncher = 'Instalowanie plików uruchomieniowych...'
    }
    'it' = @{
        ActionFailed = 'Impossibile completare l''azione selezionata.'
        AlreadyInstalled = 'LiteLauncher è già installato.'
        CheckingFound = 'trovata — l''installazione esistente verrà mantenuta'
        CheckingJava = 'Controllo dell''installazione Java...'
        CheckingNotFound = 'non trovata'
        CreateLocalShortcut = 'Crea collegamento in questa cartella'
        CreateLocalShortcutDesc = 'Crea o sostituisce un collegamento LiteLauncher nella cartella di questo installer. LiteLauncher non verrà reinstallato.'
        CreatingShortcut = 'Creazione del collegamento...'
        Done = 'Completato.'
        DownloadingJava = 'Download di Java...'
        ErrorLabel = 'Errore: '
        ExitDesc = 'Chiude l''installer senza apportare modifiche.'
        ExitInstaller = 'Esci dall''installer'
        InstallationFailed = 'Impossibile installare LiteLauncher.'
        InstalledSuccessfully = 'LiteLauncher è stato installato correttamente.'
        InstallingJava = 'Installazione di Java...'
        InteractiveRequired = 'Poiché LiteLauncher è già installato, è necessario selezionare un''azione.'
        JavaReady = 'Java è pronto.'
        LoadingJava = 'Recupero delle informazioni su Java...'
        LogLabel = 'Log:    '
        NoChanges = 'Non è stata apportata alcuna modifica.'
        OpenLauncher = 'Apri LiteLauncher'
        OpenLauncherDesc = 'Avvia LiteLauncher.'
        OpenShortcutFolder = 'Apri cartella del collegamento'
        OpenShortcutFolderDesc = 'Apre la cartella contenente il collegamento LiteLauncher installato.'
        OpeningShortcutFolder = 'Apertura della cartella del collegamento...'
        PostInstallPrompt = 'Seleziona l''azione successiva:'
        PostOpenFolderDesc = 'Apre la cartella contenente il collegamento LiteLauncher installato.'
        PreparingFiles = 'Preparazione dei file di installazione...'
        PreparingJava = 'Preparazione di Java...'
        PressLog = 'Premi L per aprire il log o qualsiasi altro tasto per chiudere l''installer.'
        PressMain = 'Seleziona un''opzione (1-5): '
        PressPost = 'Seleziona un''opzione (1-3): '
        Reinstall = 'Reinstalla LiteLauncher'
        ReinstallDesc = 'Rimuove l''installazione corrente, incluso Java 25 installato con LiteLauncher, quindi esegue una nuova installazione.'
        RemovingLauncher = 'Rimozione di LiteLauncher...'
        RemovingShortcut = 'Rimozione del collegamento...'
        ShortcutCreatedSuccessfully = 'Il collegamento LiteLauncher è stato creato correttamente.'
        ShortcutLabel = 'Collegamento: '
        StartingLauncher = 'Avvio di LiteLauncher...'
        Uninstall = 'Disinstalla LiteLauncher'
        UninstallDesc = 'Rimuove LiteLauncher, la versione di Java 25 installata con LiteLauncher e il collegamento installato.'
        UninstalledSuccessfully = 'LiteLauncher è stato disinstallato correttamente.'
        VerifyingJava = 'verifica'
        WritingBootstrap = 'Installazione dei componenti principali...'
        WritingIcons = 'Installazione delle icone...'
        WritingLauncher = 'Installazione dei file di avvio...'
    }
    'uk' = @{
        ActionFailed = 'Не вдалося виконати вибрану дію.'
        AlreadyInstalled = 'LiteLauncher уже встановлено.'
        CheckingFound = 'знайдено — наявну Java буде збережено'
        CheckingJava = 'Перевірка встановленої Java...'
        CheckingNotFound = 'не знайдено'
        CreateLocalShortcut = 'Створити ярлик у цій папці'
        CreateLocalShortcutDesc = 'Створює або замінює ярлик LiteLauncher у папці з цим інсталятором. Перевстановлення LiteLauncher не виконується.'
        CreatingShortcut = 'Створення ярлика...'
        Done = 'Готово.'
        DownloadingJava = 'Завантаження Java...'
        ErrorLabel = 'Помилка: '
        ExitDesc = 'Закриває інсталятор без внесення змін.'
        ExitInstaller = 'Вийти з інсталятора'
        InstallationFailed = 'Не вдалося встановити LiteLauncher.'
        InstalledSuccessfully = 'LiteLauncher успішно встановлено.'
        InstallingJava = 'Встановлення Java...'
        InteractiveRequired = 'Для продовження потрібно вибрати дію, оскільки LiteLauncher уже встановлено.'
        JavaReady = 'Java готова.'
        LoadingJava = 'Отримання інформації про Java...'
        LogLabel = 'Лог:     '
        NoChanges = 'Зміни не вносилися.'
        OpenLauncher = 'Відкрити LiteLauncher'
        OpenLauncherDesc = 'Запускає LiteLauncher.'
        OpenShortcutFolder = 'Відкрити папку з ярликом'
        OpenShortcutFolderDesc = 'Відкриває папку, що містить встановлений ярлик LiteLauncher.'
        OpeningShortcutFolder = 'Відкриття папки з ярликом...'
        PostInstallPrompt = 'Виберіть подальшу дію:'
        PostOpenFolderDesc = 'Відкриває папку, що містить встановлений ярлик LiteLauncher.'
        PreparingFiles = 'Підготовка файлів інсталяції...'
        PreparingJava = 'Підготовка Java...'
        PressLog = 'Натисніть L, щоб відкрити лог, або будь-яку іншу клавішу, щоб закрити інсталятор.'
        PressMain = 'Виберіть варіант (1-5): '
        PressPost = 'Виберіть варіант (1-3): '
        Reinstall = 'Перевстановити LiteLauncher'
        ReinstallDesc = 'Видаляє поточне встановлення, включно з Java 25, встановленою разом із LiteLauncher, і виконує чисте перевстановлення.'
        RemovingLauncher = 'Видалення LiteLauncher...'
        RemovingShortcut = 'Видалення ярлика...'
        ShortcutCreatedSuccessfully = 'Ярлик LiteLauncher успішно створено.'
        ShortcutLabel = 'Ярлик: '
        StartingLauncher = 'Запуск LiteLauncher...'
        Uninstall = 'Видалити LiteLauncher'
        UninstallDesc = 'Видаляє LiteLauncher, Java 25, встановлену разом із ним, і встановлений ярлик.'
        UninstalledSuccessfully = 'LiteLauncher успішно видалено.'
        VerifyingJava = 'перевірка'
        WritingBootstrap = 'Встановлення основних компонентів...'
        WritingIcons = 'Встановлення значків...'
        WritingLauncher = 'Встановлення файлів запуску...'
    }
}

function Initialize-Localization {
    $language = 'en'
    try { $language = [Globalization.CultureInfo]::CurrentUICulture.TwoLetterISOLanguageName.ToLowerInvariant() } catch {}
    if (-not $script:Translations.ContainsKey($language)) { $language = 'en' }
    $script:InstallerLanguage = $language
    $script:Messages = $script:Translations[$language]
}

Initialize-Localization

function Write-InstallerBanner {
    try { $Host.UI.RawUI.WindowTitle = 'LiteLauncher Installer' } catch {}
    try { [Console]::OutputEncoding = $script:Utf8NoBom } catch {}
    Write-Host ''
    Write-Host '  ==============================================================' -ForegroundColor DarkGray
    Write-Host '                         LiteLauncher Installer' -ForegroundColor Cyan
    Write-Host '                              Windows x64' -ForegroundColor DarkCyan
    Write-Host '  ==============================================================' -ForegroundColor DarkGray
    Write-Host ''
}

function Write-ProgressState {
    param(
        [Parameter(Mandatory = $true)][double]$Percent,
        [Parameter(Mandatory = $true)][string]$Status,
        [ConsoleColor]$Color = [ConsoleColor]::Cyan
    )

    $value = [Math]::Max(0, [Math]::Min(100, [int][Math]::Round($Percent)))
    $barWidth = 38
    $filled = [int][Math]::Floor(($barWidth * $value) / 100.0)
    $empty = $barWidth - $filled
    $bar = ('#' * $filled) + ('-' * $empty)
    $line = ('  [{0}] {1,3}%  {2}' -f $bar, $value, $Status)

    if ($line.Length -lt $script:LastProgressLength) {
        $line += (' ' * ($script:LastProgressLength - $line.Length))
    }
    $script:LastProgressLength = $line.Length

    try {
        $previous = [Console]::ForegroundColor
        [Console]::ForegroundColor = $Color
        [Console]::Write("`r" + $line)
        [Console]::ForegroundColor = $previous
    } catch {
        Write-Host $line -ForegroundColor $Color
    }
    $script:ProgressActive = $true
}

function Complete-ProgressLine {
    if ($script:ProgressActive) {
        try { [Console]::WriteLine() } catch { Write-Host '' }
        $script:ProgressActive = $false
        $script:LastProgressLength = 0
    }
}

function Write-Stage {
    param([double]$Percent, [string]$Status, [ConsoleColor]$Color = [ConsoleColor]::Cyan)
    Write-ProgressState -Percent $Percent -Status $Status -Color $Color
    Write-Log -Level 'INFO' -Message ('Progress {0}%: {1}' -f [int][Math]::Round($Percent), $Status)
}

function Initialize-Log {
    param([string]$Path)
    $directory = Split-Path -Parent $Path
    [IO.Directory]::CreateDirectory($directory) | Out-Null
    $header = 'LiteLauncher Installer log - ' + [DateTime]::UtcNow.ToString('o', [Globalization.CultureInfo]::InvariantCulture) + [Environment]::NewLine
    [IO.File]::WriteAllText($Path, $header, $script:Utf8NoBom)
}

function Write-Log {
    param(
        [Parameter(Mandatory = $true)][string]$Level,
        [Parameter(Mandatory = $true)][string]$Message,
        [Exception]$Exception
    )

    if ([string]::IsNullOrWhiteSpace($script:LogPath)) { return }
    try {
        $timestamp = [DateTime]::UtcNow.ToString('o', [Globalization.CultureInfo]::InvariantCulture)
        $text = '[{0}] {1}  {2}{3}' -f $timestamp, $Level, $Message, [Environment]::NewLine
        if ($null -ne $Exception) {
            $text += $Exception.ToString() + [Environment]::NewLine
        }
        [IO.File]::AppendAllText($script:LogPath, $text, $script:Utf8NoBom)
    } catch {}
}

function Get-InstallerPaths {
    $appData = [Environment]::GetEnvironmentVariable('APPDATA')
    if ([string]::IsNullOrWhiteSpace($appData)) {
        $appData = Join-Path ([Environment]::GetFolderPath('UserProfile')) 'AppData\Roaming'
    }

    $minecraft = Join-Path $appData '.minecraft'
    $logs = Join-Path $minecraft 'logs'
    $liteLauncher = Join-Path $minecraft 'litelauncher'
    $javaDirectory = Join-Path $liteLauncher 'java'
    $javaRoot = Join-Path $javaDirectory $script:RuntimeId
    $bootstrapDirectory = Join-Path $liteLauncher 'bootstrap'
    $launcherDirectory = Join-Path $bootstrapDirectory 'launcher'
    $iconsDirectory = Join-Path $bootstrapDirectory 'icons'

    $installerDirectory = Split-Path -Parent ([IO.Path]::GetFullPath($env:LL_INSTALLER_PATH))

    $desktop = $null
    try {
        $shell = New-Object -ComObject WScript.Shell
        $desktop = [string]$shell.SpecialFolders.Item('Desktop')
    } catch {}
    if ([string]::IsNullOrWhiteSpace($desktop)) {
        $desktop = [Environment]::GetFolderPath('Desktop')
    }
    if ([string]::IsNullOrWhiteSpace($desktop)) {
        $desktop = Join-Path ([Environment]::GetFolderPath('UserProfile')) 'Desktop'
    }

    return [pscustomobject]@{
        MinecraftDirectory = $minecraft
        LogsDirectory = $logs
        LogFile = Join-Path $logs 'litelauncher_installer.log'
        LiteLauncherDirectory = $liteLauncher
        JavaDirectory = $javaDirectory
        JavaRoot = $javaRoot
        JavaTempRoot = Join-Path $javaDirectory ($script:RuntimeId + '.tmp')
        BootstrapDirectory = $bootstrapDirectory
        BootstrapJar = Join-Path $bootstrapDirectory 'Bootstrap.jar'
        LauncherDirectory = $launcherDirectory
        IconsDirectory = $iconsDirectory
        IconFile = Join-Path $iconsDirectory 'launcher.ico'
        WindowsScript = Join-Path $bootstrapDirectory 'LiteLauncher.bat'
        InstallerDirectory = $installerDirectory
        DesktopDirectory = $desktop
        Shortcut = Join-Path $desktop 'LiteLauncher.lnk'
        LocalShortcut = Join-Path $installerDirectory 'LiteLauncher.lnk'
    }
}

function Test-LiteLauncherInstallationExists {
    param($Paths)
    return ([IO.Directory]::Exists($Paths.LiteLauncherDirectory) -or [IO.File]::Exists($Paths.LiteLauncherDirectory))
}

function Read-SingleChoiceKey {
    param([string]$AllowedChoices = '12345')
    while ($true) {
        try {
            $key = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
            $character = [char]$key.Character
        } catch {
            try {
                $character = [Console]::ReadKey($true).KeyChar
            } catch {
                $answer = Read-Host
                if ([string]::IsNullOrEmpty($answer)) { continue }
                $character = $answer[0]
            }
        }
        $choice = [string]$character
        if ($AllowedChoices.Contains($choice)) {
            Write-Host $character
            return $choice
        }
    }
}

function Get-InstallerAction {
    param($Paths)

    if (-not (Test-LiteLauncherInstallationExists -Paths $Paths)) {
        return 'Install'
    }

    Write-Host ('  ' + $script:Messages.AlreadyInstalled) -ForegroundColor Yellow
    Write-Host ''
    Write-Host ('  [1] ' + $script:Messages.Reinstall) -ForegroundColor Cyan
    Write-Host ('      ' + $script:Messages.ReinstallDesc) -ForegroundColor DarkGray
    Write-Host ('  [2] ' + $script:Messages.Uninstall) -ForegroundColor Cyan
    Write-Host ('      ' + $script:Messages.UninstallDesc) -ForegroundColor DarkGray
    Write-Host ('  [3] ' + $script:Messages.CreateLocalShortcut) -ForegroundColor Cyan
    Write-Host ('      ' + $script:Messages.CreateLocalShortcutDesc) -ForegroundColor DarkGray
    Write-Host ('  [4] ' + $script:Messages.OpenShortcutFolder) -ForegroundColor Cyan
    Write-Host ('      ' + $script:Messages.OpenShortcutFolderDesc) -ForegroundColor DarkGray
    Write-Host ('  [5] ' + $script:Messages.ExitInstaller) -ForegroundColor Cyan
    Write-Host ('      ' + $script:Messages.ExitDesc) -ForegroundColor DarkGray
    Write-Host ''
    Write-Host -NoNewline ('  ' + $script:Messages.PressMain) -ForegroundColor Yellow

    switch (Read-SingleChoiceKey -AllowedChoices '12345') {
        '1' { return 'Reinstall' }
        '2' { return 'Uninstall' }
        '3' { return 'CreateLocalShortcut' }
        '4' { return 'OpenShortcutFolder' }
        default { return 'Exit' }
    }
}

function Remove-PathCompletely {
    param([string]$Path, [string]$Description)
    if (-not ([IO.Directory]::Exists($Path) -or [IO.File]::Exists($Path))) { return }

    $lastError = $null
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        try {
            Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop
        } catch {
            $lastError = $_.Exception
        }
        if (-not ([IO.Directory]::Exists($Path) -or [IO.File]::Exists($Path))) { return }
        Start-Sleep -Milliseconds (300 * $attempt)
    }

    if ($null -ne $lastError) {
        throw (New-Object -TypeName System.IO.IOException -ArgumentList @('Unable to remove ' + $Description + ': ' + $Path, $lastError))
    }
    throw ('Unable to remove ' + $Description + ': ' + $Path)
}

function Remove-DesktopShortcutIfPresent {
    param($Paths)
    if ([IO.File]::Exists($Paths.Shortcut) -or [IO.Directory]::Exists($Paths.Shortcut)) {
        Remove-PathCompletely -Path $Paths.Shortcut -Description 'the LiteLauncher desktop shortcut'
        Write-Log -Level 'INFO' -Message ('Desktop shortcut removed: ' + $Paths.Shortcut)
    } else {
        Write-Log -Level 'INFO' -Message ('Desktop shortcut was not present: ' + $Paths.Shortcut)
    }
}

function Initialize-OperationLog {
    param($Paths, [string]$Action)
    $script:LogPath = $Paths.LogFile
    Initialize-Log -Path $script:LogPath
    Write-Log -Level 'INFO' -Message ('LiteLauncher Windows installer started. Action: ' + $Action)
    Write-Log -Level 'INFO' -Message ('Installer build: ' + $script:InstallerBuild)
    Write-Log -Level 'INFO' -Message ('Installer language: ' + $script:InstallerLanguage)
    Write-Log -Level 'INFO' -Message ('OS: ' + [Environment]::OSVersion.VersionString)
    Write-Log -Level 'INFO' -Message ('Architecture: ' + [Environment]::GetEnvironmentVariable('PROCESSOR_ARCHITECTURE'))
    Write-Log -Level 'INFO' -Message ('PowerShell: ' + $PSVersionTable.PSVersion.ToString())
    Write-Log -Level 'INFO' -Message ('Minecraft directory: ' + $Paths.MinecraftDirectory)
}

function Prepare-FullReinstallation {
    param($Paths)
    Write-Stage -Percent 2 -Status $script:Messages.RemovingShortcut -Color Yellow
    Remove-DesktopShortcutIfPresent -Paths $Paths
    Write-Stage -Percent 4 -Status $script:Messages.RemovingLauncher -Color Yellow
    Remove-PathCompletely -Path $Paths.LiteLauncherDirectory -Description 'the existing LiteLauncher installation'
    Write-Log -Level 'INFO' -Message ('Existing installation removed completely: ' + $Paths.LiteLauncherDirectory)
}

function Uninstall-LiteLauncher {
    param($Paths)
    Write-Stage -Percent 20 -Status $script:Messages.RemovingLauncher -Color Yellow
    Remove-PathCompletely -Path $Paths.LiteLauncherDirectory -Description 'the LiteLauncher installation'
    Write-Stage -Percent 75 -Status $script:Messages.RemovingShortcut -Color Yellow
    Remove-DesktopShortcutIfPresent -Paths $Paths
    Write-Stage -Percent 100 -Status $script:Messages.Done -Color Green
    Complete-ProgressLine
    Write-Host ''
    Write-Host ('  ' + $script:Messages.UninstalledSuccessfully) -ForegroundColor Green
    Write-Log -Level 'INFO' -Message 'Uninstallation completed successfully.'
    Start-Sleep -Milliseconds 650
}

function Assert-X64Windows {
    $nativeArchitecture = [Environment]::GetEnvironmentVariable('PROCESSOR_ARCHITEW6432')
    if ([string]::IsNullOrWhiteSpace($nativeArchitecture)) {
        $nativeArchitecture = [Environment]::GetEnvironmentVariable('PROCESSOR_ARCHITECTURE')
    }
    if ($nativeArchitecture -notmatch '^(AMD64|x64|x86_64)$') {
        throw ('Unsupported Windows architecture: {0}. This installer supports x64 only.' -f $nativeArchitecture)
    }
}

function Ensure-Directories {
    param($Paths)
    @(
        $Paths.BootstrapDirectory,
        $Paths.LauncherDirectory,
        $Paths.JavaDirectory,
        $Paths.IconsDirectory,
        $Paths.DesktopDirectory,
        $Paths.LogsDirectory
    ) | ForEach-Object {
        [IO.Directory]::CreateDirectory([string]$_) | Out-Null
    }
    Write-Log -Level 'INFO' -Message ('Install directory: ' + $Paths.BootstrapDirectory)
}

function Move-FileReplacing {
    param([string]$Source, [string]$Target)
    try {
        if ([IO.File]::Exists($Target)) {
            [IO.File]::Replace($Source, $Target, $null)
        } else {
            [IO.File]::Move($Source, $Target)
        }
    } catch {
        Move-Item -LiteralPath $Source -Destination $Target -Force
    }
}

function Write-EmbeddedFile {
    param([string]$Base64, [string]$Target)
    $parent = Split-Path -Parent $Target
    [IO.Directory]::CreateDirectory($parent) | Out-Null
    $temporary = $Target + '.litelauncher-install'
    Remove-Item -LiteralPath $temporary -Force -ErrorAction SilentlyContinue
    try {
        $bytes = [Convert]::FromBase64String($Base64)
        [IO.File]::WriteAllBytes($temporary, $bytes)
        Move-FileReplacing -Source $temporary -Target $Target
    } finally {
        Remove-Item -LiteralPath $temporary -Force -ErrorAction SilentlyContinue
    }
}

function Write-Utf8TextFile {
    param([string]$Path, [string]$Text)
    [IO.Directory]::CreateDirectory((Split-Path -Parent $Path)) | Out-Null
    [IO.File]::WriteAllText($Path, $Text, $script:Utf8NoBom)
}

function Find-JavaExecutable {
    param([string]$Root)

    $javaw = Join-Path $Root 'bin\javaw.exe'
    if ([IO.File]::Exists($javaw)) { return $javaw }

    $java = Join-Path $Root 'bin\java.exe'
    if ([IO.File]::Exists($java)) { return $java }

    return $null
}

function Find-FreshJavaConsoleExecutable {
    param([string]$Root)

    $java = Join-Path $Root 'bin\java.exe'
    if ([IO.File]::Exists($java)) { return $java }

    return $null
}

function Get-ExpectedInstalledJavaExecutable {
    param([string]$Root)
    return (Join-Path $Root 'bin\javaw.exe')
}

function Test-JavaRuntimeEntryExists {
    param($Paths)

    $commandProcessor = [Environment]::GetEnvironmentVariable('ComSpec')
    if ([string]::IsNullOrWhiteSpace($commandProcessor)) {
        $commandProcessor = Join-Path $env:SystemRoot 'System32\cmd.exe'
    }
    if (-not [IO.File]::Exists($commandProcessor)) {
        throw 'Windows command processor is unavailable.'
    }

    $process = New-Object Diagnostics.Process
    $startInfo = New-Object Diagnostics.ProcessStartInfo
    $startInfo.FileName = $commandProcessor
    $startInfo.Arguments = '/d /c dir /b /a'
    $startInfo.WorkingDirectory = $Paths.JavaDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $process.StartInfo = $startInfo

    Write-Log -Level 'INFO' -Message ('Checking for existing Java without probing jre-25 contents: ' + $Paths.JavaDirectory)
    try {
        if (-not $process.Start()) { throw 'Unable to start the Java directory check.' }
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        $watch = [Diagnostics.Stopwatch]::StartNew()

        while (-not $process.WaitForExit(250)) {
            $seconds = [int][Math]::Floor($watch.Elapsed.TotalSeconds)
            Write-ProgressState -Percent 38 -Status ('{0} ({1}s)' -f $script:Messages.CheckingJava, $seconds) -Color Cyan
            if ($watch.ElapsedMilliseconds -ge 5000) {
                try { $process.Kill() } catch {}
                throw 'Unable to inspect the Java installation directory within 5 seconds.'
            }
        }

        $output = $stdoutTask.Result
        $errorText = $stderrTask.Result
        if ($process.ExitCode -ne 0) {
            if ([string]::IsNullOrWhiteSpace($errorText)) { $errorText = 'dir exited with code ' + $process.ExitCode + '.' }
            throw ('Unable to inspect the Java installation directory. ' + $errorText.Trim())
        }

        foreach ($line in ($output -split "`r?`n")) {
            if ($line -ceq $script:RuntimeId) {
                Write-Log -Level 'INFO' -Message ('Java runtime directory entry exists: ' + $Paths.JavaRoot)
                return $true
            }
        }
        Write-Log -Level 'INFO' -Message ('Java runtime directory entry is absent: ' + $Paths.JavaRoot)
        return $false
    } finally {
        try { if (-not $process.HasExited) { $process.Kill() } } catch {}
        $process.Dispose()
    }
}

function Remove-PathQuietly {
    param([string]$Path)
    if ([string]::IsNullOrWhiteSpace($Path)) { return }
    try { Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop } catch {}
}

function Cleanup-JavaTemporaryFiles {
    param($Paths, [string]$Archive)
    Remove-PathQuietly -Path $Paths.JavaTempRoot
    if (-not [string]::IsNullOrWhiteSpace($Archive)) {
        Remove-PathQuietly -Path $Archive
        Remove-PathQuietly -Path ($Archive + '.litelauncher-download')
    }
    Remove-PathQuietly -Path (Join-Path $Paths.JavaDirectory ($script:RuntimeId + '.zip'))
    Remove-PathQuietly -Path (Join-Path $Paths.JavaDirectory ($script:RuntimeId + '.tar.gz'))
    Remove-PathQuietly -Path (Join-Path $Paths.JavaDirectory ($script:RuntimeId + '.zip.litelauncher-download'))
    Remove-PathQuietly -Path (Join-Path $Paths.JavaDirectory ($script:RuntimeId + '.tar.gz.litelauncher-download'))
}

function Enable-Tls12 {
    try {
        [Net.ServicePointManager]::SecurityProtocol =
            [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
    } catch {
        throw 'TLS 1.2 could not be enabled in Windows PowerShell.'
    }
}

function Request-ManifestText {
    param([string]$Url)

    $lastError = $null
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $request = $null
        $response = $null
        $reader = $null
        $async = $null
        try {
            Write-Log -Level 'INFO' -Message ('Java manifest request attempt {0}: {1}' -f $attempt, $Url)
            $request = [Net.HttpWebRequest]::Create($Url)
            $request.Method = 'GET'
            $request.Accept = 'application/json'
            $request.UserAgent = 'LiteLauncher'
            $request.AllowAutoRedirect = $true
            $request.Timeout = 35000
            $request.ReadWriteTimeout = 30000

            $async = $request.BeginGetResponse($null, $null)
            $watch = [Diagnostics.Stopwatch]::StartNew()
            while (-not $async.AsyncWaitHandle.WaitOne(1000)) {
                $seconds = [int][Math]::Floor($watch.Elapsed.TotalSeconds)
                Write-ProgressState -Percent 40 -Status ('{0} ({1}s)' -f $script:Messages.LoadingJava, $seconds) -Color Cyan
                if ($watch.ElapsedMilliseconds -ge 40000) {
                    try { $request.Abort() } catch {}
                    throw 'Java manifest request exceeded the 40-second watchdog.'
                }
            }

            $response = [Net.HttpWebResponse]$request.EndGetResponse($async)
            $status = [int]$response.StatusCode
            if ($status -lt 200 -or $status -ge 300) {
                throw ('Java manifest request failed with HTTP {0}.' -f $status)
            }
            $reader = New-Object IO.StreamReader($response.GetResponseStream(), [Text.Encoding]::UTF8, $true)
            $text = $reader.ReadToEnd()
            if ([string]::IsNullOrWhiteSpace($text)) { throw 'Java runtime manifest response was empty.' }
            Write-Log -Level 'INFO' -Message ('Java manifest response: HTTP {0}; bytes={1}; content-type={2}' -f $status, [Text.Encoding]::UTF8.GetByteCount($text), $response.ContentType)
            return $text
        } catch {
            $lastError = $_.Exception
            Write-Log -Level 'ERROR' -Message ('Java manifest request attempt {0} failed.' -f $attempt) -Exception $_.Exception
            if ($attempt -lt 3) {
                Write-ProgressState -Percent 40 -Status $script:Messages.LoadingJava -Color Yellow
                Start-Sleep -Milliseconds (1000 * $attempt)
            }
        } finally {
            if ($null -ne $reader) { $reader.Dispose() }
            if ($null -ne $response) { $response.Dispose() }
            if ($null -ne $async) { try { $async.AsyncWaitHandle.Close() } catch {} }
            if ($null -ne $request) { try { $request.Abort() } catch {} }
        }
    }
    throw (New-Object -TypeName System.IO.IOException -ArgumentList @('Unable to load Java runtime manifest.', $lastError))
}

function Get-JsonPropertyValue {
    param($Object, [string]$Name, [string]$ErrorMessage)
    if ($null -eq $Object) { throw $ErrorMessage }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) { throw $ErrorMessage }
    return $property.Value
}

function Test-JsonIntegerValue {
    param($Value)
    if ($null -eq $Value) { return $false }
    return @(
        'System.SByte', 'System.Byte', 'System.Int16', 'System.UInt16',
        'System.Int32', 'System.UInt32', 'System.Int64', 'System.UInt64'
    ) -contains $Value.GetType().FullName
}

function Resolve-JavaPackage {
    $manifestText = Request-ManifestText -Url $script:ManifestUrl
    try {
        $manifest = $manifestText | ConvertFrom-Json
    } catch {
        $preview = $manifestText.Replace("`r", ' ').Replace("`n", ' ').Replace("`t", ' ')
        if ($preview.Length -gt 200) { $preview = $preview.Substring(0, 200) }
        Write-Log -Level 'ERROR' -Message ('Invalid manifest preview: ' + $preview)
        throw (New-Object -TypeName System.IO.IOException -ArgumentList @('Invalid Java runtime manifest JSON.', $_.Exception))
    }

    $schemaVersion = Get-JsonPropertyValue -Object $manifest -Name 'schemaVersion' -ErrorMessage 'Invalid Java manifest field: schemaVersion.'
    if (-not (Test-JsonIntegerValue -Value $schemaVersion) -or [int64]$schemaVersion -ne 1) {
        throw 'Unsupported Java manifest schema version.'
    }

    $runtimes = Get-JsonPropertyValue -Object $manifest -Name 'runtimes' -ErrorMessage 'Invalid Java manifest field: runtimes.'
    $version = Get-JsonPropertyValue -Object $runtimes -Name '25' -ErrorMessage 'Java 25 is unavailable for windows/x64.'
    $windows = Get-JsonPropertyValue -Object $version -Name 'windows' -ErrorMessage 'Java 25 is unavailable for windows/x64.'
    $runtime = Get-JsonPropertyValue -Object $windows -Name 'x64' -ErrorMessage 'Java 25 is unavailable for windows/x64.'

    $name = [string](Get-JsonPropertyValue -Object $runtime -Name 'name' -ErrorMessage 'Invalid Java manifest field: name.')
    $url = [string](Get-JsonPropertyValue -Object $runtime -Name 'url' -ErrorMessage 'Invalid Java manifest field: url.')
    $sha1 = [string](Get-JsonPropertyValue -Object $runtime -Name 'sha1' -ErrorMessage 'Invalid Java manifest field: sha1.')
    $sizeValue = Get-JsonPropertyValue -Object $runtime -Name 'size' -ErrorMessage 'Invalid Java manifest field: size.'

    if (-not (Test-JsonIntegerValue -Value $sizeValue)) { throw 'Invalid Java manifest field: size.' }
    try { $size = [int64]$sizeValue } catch { throw 'Invalid Java manifest field: size.' }
    if ([string]::IsNullOrWhiteSpace($name) -or $name.Contains('/') -or $name.Contains('\') -or $name -match '[\r\n\t]') {
        throw 'Invalid Java package name in manifest.'
    }
    $lowerName = $name.ToLowerInvariant()
    if (-not ($lowerName.EndsWith('.zip') -or $lowerName.EndsWith('.tar.gz'))) {
        throw 'Unsupported Java archive format in manifest.'
    }
    if ([string]::IsNullOrWhiteSpace($url) -or $url -match '[\r\n\t]') { throw 'Invalid Java package URL in manifest.' }
    if ($sha1 -notmatch '^[0-9a-fA-F]{40}$') { throw 'Invalid Java package SHA-1 in manifest.' }
    if ($size -le 0) { throw 'Invalid Java package size in manifest.' }

    $package = [pscustomobject]@{
        Name = $name
        Url = $url
        Sha1 = $sha1.ToLowerInvariant()
        Size = $size
        Extension = $(if ($lowerName.EndsWith('.zip')) { '.zip' } else { '.tar.gz' })
    }
    Write-Log -Level 'INFO' -Message ('Resolved Java package: name={0}; bytes={1}; sha1={2}; url={3}' -f $package.Name, $package.Size, $package.Sha1, $package.Url)
    return $package
}

function Get-Sha1 {
    param([string]$Path)
    $stream = $null
    $algorithm = $null
    try {
        $stream = [IO.File]::OpenRead($Path)
        $algorithm = [Security.Cryptography.SHA1]::Create()
        $hash = $algorithm.ComputeHash($stream)
        return ([BitConverter]::ToString($hash)).Replace('-', '').ToLowerInvariant()
    } finally {
        if ($null -ne $algorithm) { $algorithm.Dispose() }
        if ($null -ne $stream) { $stream.Dispose() }
    }
}

function Test-JavaArchive {
    param([string]$Path, [int64]$Size, [string]$Sha1)
    if (-not [IO.File]::Exists($Path)) { return $false }
    if ((New-Object IO.FileInfo($Path)).Length -ne $Size) { return $false }
    return (Get-Sha1 -Path $Path) -ieq $Sha1
}

function Download-JavaArchive {
    param($Package, [string]$Archive)

    $temporary = $Archive + '.litelauncher-download'
    $lastError = $null

    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $request = $null
        $response = $null
        $input = $null
        $output = $null
        $responseAsync = $null
        $readAsync = $null
        Remove-PathQuietly -Path $temporary
        try {
            Write-Log -Level 'INFO' -Message ('Download attempt {0} for Java: {1}' -f $attempt, $Package.Url)
            Write-ProgressState -Percent 42 -Status ($script:Messages.DownloadingJava + ' 0%') -Color Cyan

            $request = [Net.HttpWebRequest]::Create($Package.Url)
            $request.Method = 'GET'
            $request.UserAgent = 'LiteLauncher/' + $PSVersionTable.PSVersion.ToString()
            $request.AllowAutoRedirect = $true
            $request.Timeout = 180000
            $request.ReadWriteTimeout = 180000

            $responseAsync = $request.BeginGetResponse($null, $null)
            $connectWatch = [Diagnostics.Stopwatch]::StartNew()
            while (-not $responseAsync.AsyncWaitHandle.WaitOne(200)) {
                Write-ProgressState -Percent 42 -Status ($script:Messages.DownloadingJava + ' 0%') -Color Cyan
                if ($connectWatch.ElapsedMilliseconds -ge 190000) {
                    try { $request.Abort() } catch {}
                    throw 'Java download connection exceeded the 190-second watchdog.'
                }
            }

            $response = [Net.HttpWebResponse]$request.EndGetResponse($responseAsync)
            $status = [int]$response.StatusCode
            Write-Log -Level 'INFO' -Message ('HTTP {0}: {1}' -f $status, $Package.Url)
            if ($status -lt 200 -or $status -ge 300) {
                throw ('Unable to download Java: HTTP {0}.' -f $status)
            }

            $input = $response.GetResponseStream()
            $output = New-Object IO.FileStream($temporary, [IO.FileMode]::Create, [IO.FileAccess]::Write, [IO.FileShare]::None)
            $buffer = New-Object byte[] (64 * 1024)
            [int64]$downloaded = 0
            $totalWatch = [Diagnostics.Stopwatch]::StartNew()
            $inactivityWatch = [Diagnostics.Stopwatch]::StartNew()

            while ($true) {
                $readAsync = $input.BeginRead($buffer, 0, $buffer.Length, $null, $null)
                while (-not $readAsync.AsyncWaitHandle.WaitOne(200)) {
                    $ratio = [Math]::Max(0.0, [Math]::Min(1.0, $downloaded / [double]$Package.Size))
                    $overall = 42.0 + (25.8 * $ratio)
                    $downloadPercent = [int][Math]::Floor($ratio * 100.0)
                    Write-ProgressState -Percent $overall -Status ('{0} {1}%' -f $script:Messages.DownloadingJava, $downloadPercent) -Color Cyan
                    if ($inactivityWatch.ElapsedMilliseconds -ge 180000) {
                        try { $request.Abort() } catch {}
                        throw 'Java download received no data for 180 seconds.'
                    }
                    if ($totalWatch.ElapsedMilliseconds -ge 1800000) {
                        try { $request.Abort() } catch {}
                        throw 'Java download exceeded the 30-minute limit.'
                    }
                }

                $read = $input.EndRead($readAsync)
                try { $readAsync.AsyncWaitHandle.Close() } catch {}
                $readAsync = $null
                if ($read -le 0) { break }
                $output.Write($buffer, 0, $read)
                $downloaded += $read
                $inactivityWatch.Restart()

                $ratio = [Math]::Max(0.0, [Math]::Min(1.0, $downloaded / [double]$Package.Size))
                $overall = 42.0 + (25.8 * $ratio)
                $downloadPercent = [int][Math]::Floor($ratio * 100.0)
                Write-ProgressState -Percent $overall -Status ('{0} {1}%' -f $script:Messages.DownloadingJava, $downloadPercent) -Color Cyan
            }

            $output.Flush()
            $output.Dispose()
            $output = $null
            $input.Dispose()
            $input = $null
            $response.Dispose()
            $response = $null

            if (-not (Test-JavaArchive -Path $temporary -Size $Package.Size -Sha1 $Package.Sha1)) {
                throw 'Downloaded Java archive failed size or SHA-1 verification.'
            }

            Move-FileReplacing -Source $temporary -Target $Archive
            Write-Log -Level 'INFO' -Message ('Downloaded and verified Java archive: ' + $Archive)
            Write-ProgressState -Percent 68 -Status ($script:Messages.DownloadingJava + ' 100%') -Color Cyan
            return
        } catch {
            $lastError = $_.Exception
            Write-Log -Level 'ERROR' -Message ('Java download attempt {0} failed.' -f $attempt) -Exception $_.Exception
            Remove-PathQuietly -Path $temporary
            if ($attempt -lt 3) {
                Write-ProgressState -Percent 42 -Status $script:Messages.DownloadingJava -Color Yellow
                Start-Sleep -Milliseconds (1000 * $attempt)
            }
        } finally {
            if ($null -ne $readAsync) { try { $readAsync.AsyncWaitHandle.Close() } catch {} }
            if ($null -ne $responseAsync) { try { $responseAsync.AsyncWaitHandle.Close() } catch {} }
            if ($null -ne $output) { $output.Dispose() }
            if ($null -ne $input) { $input.Dispose() }
            if ($null -ne $response) { $response.Dispose() }
            if ($null -ne $request) { try { $request.Abort() } catch {} }
        }
    }

    throw (New-Object -TypeName System.IO.IOException -ArgumentList @('Unable to download Java runtime.', $lastError))
}

function Get-StrippedArchivePath {
    param([string]$EntryName)
    if ($null -eq $EntryName) { return '' }
    $normalized = $EntryName.Replace('\', '/')
    $slash = $normalized.IndexOf('/')
    if ($slash -lt 0) { return '' }
    return $normalized.Substring($slash + 1)
}

function Get-SafeExtractionPath {
    param([string]$Root, [string]$Relative)
    if ([string]::IsNullOrWhiteSpace($Relative)) { return $null }
    $rootFull = [IO.Path]::GetFullPath($Root)
    $separator = [string][IO.Path]::DirectorySeparatorChar
    if (-not $rootFull.EndsWith($separator)) {
        $rootFull += $separator
    }
    $candidate = [IO.Path]::GetFullPath((Join-Path $Root ($Relative.Replace('/', $separator))))
    if (-not $candidate.StartsWith($rootFull, [StringComparison]::OrdinalIgnoreCase)) { return $null }
    return $candidate
}

function Extract-ZipArchive {
    param([string]$Archive, [string]$Target)
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Directory]::CreateDirectory($Target) | Out-Null
    $zip = [IO.Compression.ZipFile]::OpenRead($Archive)
    try {
        foreach ($entry in $zip.Entries) {
            $relative = Get-StrippedArchivePath -EntryName $entry.FullName
            $destination = Get-SafeExtractionPath -Root $Target -Relative $relative
            if ([string]::IsNullOrWhiteSpace($destination)) { continue }

            if ($entry.FullName.EndsWith('/')) {
                [IO.Directory]::CreateDirectory($destination) | Out-Null
                continue
            }

            [IO.Directory]::CreateDirectory((Split-Path -Parent $destination)) | Out-Null
            $sourceStream = $entry.Open()
            $destinationStream = $null
            try {
                $destinationStream = New-Object IO.FileStream($destination, [IO.FileMode]::Create, [IO.FileAccess]::Write, [IO.FileShare]::None)
                $sourceStream.CopyTo($destinationStream)
            } finally {
                if ($null -ne $destinationStream) { $destinationStream.Dispose() }
                $sourceStream.Dispose()
            }
        }
    } finally {
        $zip.Dispose()
    }
}

function Read-ExactBytes {
    param([IO.Stream]$Stream, [byte[]]$Buffer, [int]$Count)
    $offset = 0
    while ($offset -lt $Count) {
        $read = $Stream.Read($Buffer, $offset, $Count - $offset)
        if ($read -le 0) { break }
        $offset += $read
    }
    return $offset
}

function Test-EmptyTarBlock {
    param([byte[]]$Buffer)
    foreach ($value in $Buffer) { if ($value -ne 0) { return $false } }
    return $true
}

function Get-TarString {
    param([byte[]]$Buffer, [int]$Offset, [int]$Length)
    $end = $Offset
    $limit = $Offset + $Length
    while ($end -lt $limit -and $Buffer[$end] -ne 0) { $end++ }
    return [Text.Encoding]::UTF8.GetString($Buffer, $Offset, $end - $Offset).Trim()
}

function Get-TarOctal {
    param([byte[]]$Buffer, [int]$Offset, [int]$Length)
    $text = (Get-TarString -Buffer $Buffer -Offset $Offset -Length $Length).Trim()
    if ([string]::IsNullOrWhiteSpace($text)) { return [int64]0 }
    return [Convert]::ToInt64($text, 8)
}

function Copy-ExactBytes {
    param([IO.Stream]$Input, [IO.Stream]$Output, [int64]$Count)
    $buffer = New-Object byte[] (64 * 1024)
    [int64]$remaining = $Count
    while ($remaining -gt 0) {
        $requested = [int][Math]::Min($buffer.Length, $remaining)
        $read = $Input.Read($buffer, 0, $requested)
        if ($read -le 0) { throw (New-Object -TypeName System.IO.EndOfStreamException) }
        $Output.Write($buffer, 0, $read)
        $remaining -= $read
    }
}

function Skip-ExactBytes {
    param([IO.Stream]$Input, [int64]$Count)
    $buffer = New-Object byte[] 8192
    [int64]$remaining = $Count
    while ($remaining -gt 0) {
        $requested = [int][Math]::Min($buffer.Length, $remaining)
        $read = $Input.Read($buffer, 0, $requested)
        if ($read -le 0) { throw (New-Object -TypeName System.IO.EndOfStreamException) }
        $remaining -= $read
    }
}

function Extract-TarGzArchive {
    param([string]$Archive, [string]$Target)
    [IO.Directory]::CreateDirectory($Target) | Out-Null
    $fileStream = [IO.File]::OpenRead($Archive)
    $gzip = New-Object IO.Compression.GZipStream($fileStream, [IO.Compression.CompressionMode]::Decompress)
    try {
        $header = New-Object byte[] 512
        while ((Read-ExactBytes -Stream $gzip -Buffer $header -Count 512) -eq 512) {
            if (Test-EmptyTarBlock -Buffer $header) { break }
            $name = Get-StrippedArchivePath -EntryName (Get-TarString -Buffer $header -Offset 0 -Length 100)
            $size = Get-TarOctal -Buffer $header -Offset 124 -Length 12
            $type = [char]$header[156]
            $destination = Get-SafeExtractionPath -Root $Target -Relative $name

            if ([string]::IsNullOrWhiteSpace($destination)) {
                Skip-ExactBytes -Input $gzip -Count $size
            } elseif ($type -eq '5') {
                [IO.Directory]::CreateDirectory($destination) | Out-Null
            } elseif ($type -eq '0' -or [int]$header[156] -eq 0) {
                [IO.Directory]::CreateDirectory((Split-Path -Parent $destination)) | Out-Null
                $output = New-Object IO.FileStream($destination, [IO.FileMode]::Create, [IO.FileAccess]::Write, [IO.FileShare]::None)
                try { Copy-ExactBytes -Input $gzip -Output $output -Count $size } finally { $output.Dispose() }
            } else {
                Skip-ExactBytes -Input $gzip -Count $size
            }

            $padding = (512 - ($size % 512)) % 512
            if ($padding -gt 0) { Skip-ExactBytes -Input $gzip -Count $padding }
            $header = New-Object byte[] 512
        }
    } finally {
        $gzip.Dispose()
        $fileStream.Dispose()
    }
}

function Verify-FreshJava {
    param([string]$JavaExecutable)

    $process = New-Object Diagnostics.Process
    $startInfo = New-Object Diagnostics.ProcessStartInfo
    $startInfo.FileName = $JavaExecutable
    $startInfo.Arguments = '-version'
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $process.StartInfo = $startInfo

    try {
        if (-not $process.Start()) { throw 'The installed Java runtime could not be started.' }
        $stdoutTask = $process.StandardOutput.ReadToEndAsync()
        $stderrTask = $process.StandardError.ReadToEndAsync()
        $watch = [Diagnostics.Stopwatch]::StartNew()
        while (-not $process.WaitForExit(1000)) {
            $seconds = [int][Math]::Floor($watch.Elapsed.TotalSeconds)
            Write-ProgressState -Percent 72 -Status ('{0} {1} ({2}s)' -f $script:Messages.InstallingJava, $script:Messages.VerifyingJava, $seconds) -Color Yellow
            if ($watch.ElapsedMilliseconds -ge 20000) {
                try { $process.Kill() } catch {}
                throw 'The installed Java runtime did not start within 20 seconds.'
            }
        }

        $stdout = $stdoutTask.Result
        $stderr = $stderrTask.Result
        if ($process.ExitCode -ne 0) {
            $details = ($stderr + [Environment]::NewLine + $stdout).Trim()
            if ([string]::IsNullOrWhiteSpace($details)) { $details = 'Java exited with code ' + $process.ExitCode + '.' }
            throw ('The installed Java runtime failed validation. ' + (($details -split "`r?`n")[-1]))
        }
        Write-Log -Level 'INFO' -Message ('Fresh Java verification succeeded: ' + $JavaExecutable)
    } finally {
        try { if (-not $process.HasExited) { $process.Kill() } } catch {}
        $process.Dispose()
    }
}

function Install-JavaRuntime {
    param($Paths)

    Write-ProgressState -Percent 36 -Status $script:Messages.PreparingJava -Color Cyan
    Cleanup-JavaTemporaryFiles -Paths $Paths -Archive $null
    Write-ProgressState -Percent 38 -Status $script:Messages.CheckingJava -Color Cyan

    if (Test-JavaRuntimeEntryExists -Paths $Paths) {
        Write-ProgressState -Percent 39 -Status ($script:Messages.CheckingJava + ' ' + $script:Messages.CheckingFound) -Color Cyan
        Write-Log -Level 'INFO' -Message ('Existing Java runtime entry found and preserved without probing its contents: ' + $Paths.JavaRoot)
        Write-Stage -Percent 75 -Status $script:Messages.JavaReady -Color Green
        return (Get-ExpectedInstalledJavaExecutable -Root $Paths.JavaRoot)
    }

    Write-ProgressState -Percent 39 -Status ($script:Messages.CheckingJava + ' ' + $script:Messages.CheckingNotFound) -Color Cyan
    Write-ProgressState -Percent 40 -Status $script:Messages.LoadingJava -Color Cyan
    Write-Log -Level 'INFO' -Message ('Loading Java runtime manifest: ' + $script:ManifestUrl)
    $package = Resolve-JavaPackage
    $archive = Join-Path $Paths.JavaDirectory ($script:RuntimeId + $package.Extension)
    Cleanup-JavaTemporaryFiles -Paths $Paths -Archive $archive

    Write-Log -Level 'INFO' -Message ('Java runtime missing, installing: {0} -> {1}' -f $script:RuntimeId, $Paths.JavaRoot)
    Remove-PathQuietly -Path $Paths.JavaRoot

    try {
        Download-JavaArchive -Package $package -Archive $archive
        Write-Stage -Percent 70 -Status $script:Messages.InstallingJava -Color Yellow
        Write-Log -Level 'INFO' -Message ('Extracting Java archive: {0} -> {1}' -f $archive, $Paths.JavaTempRoot)
        Remove-PathQuietly -Path $Paths.JavaTempRoot

        if ($package.Extension -eq '.zip') {
            Extract-ZipArchive -Archive $archive -Target $Paths.JavaTempRoot
        } else {
            Extract-TarGzArchive -Archive $archive -Target $Paths.JavaTempRoot
        }

        $temporaryJavaConsole = Find-FreshJavaConsoleExecutable -Root $Paths.JavaTempRoot
        $temporaryJava = Find-JavaExecutable -Root $Paths.JavaTempRoot
        if ([string]::IsNullOrWhiteSpace($temporaryJavaConsole) -or [string]::IsNullOrWhiteSpace($temporaryJava)) {
            throw 'Java error.'
        }
        Verify-FreshJava -JavaExecutable $temporaryJavaConsole

        Remove-PathQuietly -Path $Paths.JavaRoot
        Move-Item -LiteralPath $Paths.JavaTempRoot -Destination $Paths.JavaRoot
        Remove-PathQuietly -Path $archive

        $installedJava = Find-JavaExecutable -Root $Paths.JavaRoot
        if ([string]::IsNullOrWhiteSpace($installedJava)) { throw 'Java error.' }

        Write-Log -Level 'INFO' -Message ('Java runtime installed: {0} -> {1}' -f $script:RuntimeId, $installedJava)
        Write-Stage -Percent 75 -Status $script:Messages.JavaReady -Color Green
        return $installedJava
    } catch {
        Write-Log -Level 'ERROR' -Message ('Java runtime installation failed: ' + $script:RuntimeId) -Exception $_.Exception
        Cleanup-JavaTemporaryFiles -Paths $Paths -Archive $archive
        throw
    } finally {
        Remove-PathQuietly -Path $archive
        Remove-PathQuietly -Path ($archive + '.litelauncher-download')
    }
}

function Create-WindowsLauncherScript {
    param($Paths)
    $scriptText = @"
@echo off
setlocal
set "APP_HOME=%~dp0"
set "JAVA_EXE=%APP_HOME%..\java\jre-25\bin\javaw.exe"
if not exist "%JAVA_EXE%" set "JAVA_EXE=%APP_HOME%..\java\jre-25\bin\java.exe"
if not exist "%JAVA_EXE%" (
  echo LiteLauncher Java 25 was not found. Reinstall LiteLauncher and try again.
  pause
  exit /b 1
)
start "" /D "%APP_HOME%" "%JAVA_EXE%" -jar "%APP_HOME%Bootstrap.jar"
"@
    $scriptText = $scriptText.Replace("`r`n", "`n").Replace("`n", "`r`n")
    Write-Utf8TextFile -Path $Paths.WindowsScript -Text $scriptText
    Write-Log -Level 'INFO' -Message ('Launch script created: ' + $Paths.WindowsScript)
}

function Create-WindowsShortcut {
    param($Paths, [string]$ShortcutPath, [string]$JavaExecutable)
    if ([IO.File]::Exists($ShortcutPath) -or [IO.Directory]::Exists($ShortcutPath)) {
        Remove-PathCompletely -Path $ShortcutPath -Description 'the existing shortcut'
    }

    $parent = Split-Path -Parent $ShortcutPath
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        [IO.Directory]::CreateDirectory($parent) | Out-Null
    }

    $lastError = $null
    for ($attempt = 1; $attempt -le 2; $attempt++) {
        $shell = $null
        $shortcut = $null
        try {
            $shell = New-Object -ComObject WScript.Shell
            $shortcut = $shell.CreateShortcut($ShortcutPath)
            $shortcut.TargetPath = $JavaExecutable
            $shortcut.Arguments = '-jar "' + $Paths.BootstrapJar + '"'
            $shortcut.WorkingDirectory = $Paths.BootstrapDirectory
            $shortcut.IconLocation = $Paths.IconFile
            $shortcut.WindowStyle = 1
            $shortcut.Save()

            if ([IO.File]::Exists($ShortcutPath)) {
                Write-Log -Level 'INFO' -Message ('Windows shortcut created: ' + $ShortcutPath)
                return
            }
            $lastError = New-Object -TypeName System.IO.IOException -ArgumentList @('Windows did not create the requested shortcut: ' + $ShortcutPath)
        } catch {
            $lastError = $_.Exception
            Write-Log -Level 'ERROR' -Message ('Windows shortcut creation attempt {0} failed: {1}' -f $attempt, $ShortcutPath) -Exception $_.Exception
        } finally {
            if ($null -ne $shortcut) { try { [Runtime.InteropServices.Marshal]::FinalReleaseComObject($shortcut) | Out-Null } catch {} }
            if ($null -ne $shell) { try { [Runtime.InteropServices.Marshal]::FinalReleaseComObject($shell) | Out-Null } catch {} }
        }
        if ($attempt -lt 2) { Start-Sleep -Milliseconds 250 }
    }

    if ($null -ne $lastError) {
        throw (New-Object -TypeName System.IO.IOException -ArgumentList @('Windows could not create the requested shortcut: ' + $ShortcutPath, $lastError))
    }
    throw ('Windows could not create the requested shortcut: ' + $ShortcutPath)
}

function Create-LocalShortcutOnly {
    param($Paths)
    $javaExecutable = Find-JavaExecutable -Root $Paths.JavaRoot
    if ([string]::IsNullOrWhiteSpace($javaExecutable)) {
        throw 'Installed LiteLauncher Java 25 was not found. Reinstall LiteLauncher and try again.'
    }
    if (-not [IO.File]::Exists($Paths.BootstrapJar)) {
        throw 'Installed LiteLauncher Bootstrap.jar was not found. Reinstall LiteLauncher and try again.'
    }
    if (-not [IO.File]::Exists($Paths.IconFile)) {
        throw 'Installed LiteLauncher shortcut icon was not found. Reinstall LiteLauncher and try again.'
    }

    Write-Stage -Percent 40 -Status $script:Messages.CreatingShortcut
    Create-WindowsShortcut -Paths $Paths -ShortcutPath $Paths.LocalShortcut -JavaExecutable $javaExecutable
    Write-Stage -Percent 100 -Status $script:Messages.Done -Color Green
    Complete-ProgressLine
    Write-Host ''
    Write-Host ('  ' + $script:Messages.ShortcutCreatedSuccessfully) -ForegroundColor Green
    Write-Host ('  ' + $script:Messages.ShortcutLabel + $Paths.LocalShortcut) -ForegroundColor DarkGray
    Write-Log -Level 'INFO' -Message ('Local shortcut creation completed successfully: ' + $Paths.LocalShortcut)
    Start-Sleep -Milliseconds 650
}

function Start-LiteLauncher {
    param($Paths, [string]$JavaExecutable)
    $arguments = '-jar "' + $Paths.BootstrapJar + '"'
    Start-Process -FilePath $JavaExecutable -ArgumentList $arguments -WorkingDirectory $Paths.BootstrapDirectory | Out-Null
    Write-Log -Level 'INFO' -Message ('LiteLauncher started: ' + $JavaExecutable)
}

function Open-ShortcutFolder {
    param($Paths)
    [IO.Directory]::CreateDirectory($Paths.DesktopDirectory) | Out-Null
    $explorer = Join-Path $env:WINDIR 'explorer.exe'
    if (-not [IO.File]::Exists($explorer)) { $explorer = 'explorer.exe' }
    Start-Process -FilePath $explorer -ArgumentList ('"' + $Paths.DesktopDirectory + '"') | Out-Null
    Write-Log -Level 'INFO' -Message ('Shortcut folder opened: ' + $Paths.DesktopDirectory)
}

function Open-ShortcutFolderOnly {
    param($Paths)
    Write-Host ''
    Write-Host ('  ' + $script:Messages.OpeningShortcutFolder) -ForegroundColor Cyan
    Open-ShortcutFolder -Paths $Paths
    Start-Sleep -Milliseconds 350
}

function Show-PostInstallMenu {
    param($Paths, [string]$JavaExecutable)

    Write-Host ''
    Write-Host ('  ' + $script:Messages.InstalledSuccessfully) -ForegroundColor Green
    Write-Host ('  ' + $script:Messages.PostInstallPrompt) -ForegroundColor Yellow
    Write-Host ''
    Write-Host ('  [1] ' + $script:Messages.OpenLauncher) -ForegroundColor Cyan
    Write-Host ('      ' + $script:Messages.OpenLauncherDesc) -ForegroundColor DarkGray
    Write-Host ('  [2] ' + $script:Messages.OpenShortcutFolder) -ForegroundColor Cyan
    Write-Host ('      ' + $script:Messages.PostOpenFolderDesc) -ForegroundColor DarkGray
    Write-Host ('  [3] ' + $script:Messages.ExitInstaller) -ForegroundColor Cyan
    Write-Host ('      ' + $script:Messages.ExitDesc) -ForegroundColor DarkGray
    Write-Host ''

    while ($true) {
        Write-Host -NoNewline ('  ' + $script:Messages.PressPost) -ForegroundColor Yellow
        switch (Read-SingleChoiceKey -AllowedChoices '123') {
            '1' {
                Write-Host ('  ' + $script:Messages.StartingLauncher) -ForegroundColor Green
                try {
                    Start-LiteLauncher -Paths $Paths -JavaExecutable $JavaExecutable
                    return
                } catch {
                    Write-Log -Level 'ERROR' -Message 'Post-install launcher start failed.' -Exception $_.Exception
                    Write-Host ('  ' + $script:Messages.ActionFailed) -ForegroundColor Red
                }
            }
            '2' {
                Write-Host ('  ' + $script:Messages.OpeningShortcutFolder) -ForegroundColor Cyan
                try {
                    Open-ShortcutFolder -Paths $Paths
                    return
                } catch {
                    Write-Log -Level 'ERROR' -Message 'Post-install shortcut folder open failed.' -Exception $_.Exception
                    Write-Host ('  ' + $script:Messages.ActionFailed) -ForegroundColor Red
                }
            }
            default { return }
        }
    }
}

function Show-InstallationFailure {
    param([Exception]$Exception)
    Complete-ProgressLine
    Write-Log -Level 'ERROR' -Message 'Installation failed.' -Exception $Exception
    Write-Host ''
    Write-Host ('  ' + $script:Messages.InstallationFailed) -ForegroundColor Red
    Write-Host ('  ' + $script:Messages.ErrorLabel + $Exception.Message) -ForegroundColor Red
    if (-not [string]::IsNullOrWhiteSpace($script:LogPath)) {
        Write-Host ('  ' + $script:Messages.LogLabel + $script:LogPath) -ForegroundColor DarkGray
    }
    Write-Host ''
    Write-Host ('  ' + $script:Messages.PressLog) -ForegroundColor Yellow

    try {
        $key = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
        if ($key.Character -eq 'l' -or $key.Character -eq 'L') {
            Start-Process -FilePath $script:LogPath | Out-Null
        }
    } catch {
        try {
            $answer = Read-Host
            if ($answer -ieq 'L') { Start-Process -FilePath $script:LogPath | Out-Null }
        } catch {}
    }
}

function Install-LiteLauncher {
    param($Paths)

    Write-Stage -Percent 5 -Status $script:Messages.PreparingFiles
    Ensure-Directories -Paths $Paths

    Write-Stage -Percent 20 -Status $script:Messages.WritingBootstrap
    Write-EmbeddedFile -Base64 $script:BootstrapBase64 -Target $Paths.BootstrapJar
    Write-Log -Level 'INFO' -Message ('Bootstrap installed: ' + $Paths.BootstrapJar)

    Write-Stage -Percent 30 -Status $script:Messages.WritingIcons
    Write-EmbeddedFile -Base64 $script:IconBase64 -Target $Paths.IconFile
    Write-Log -Level 'INFO' -Message ('Shortcut icon installed: ' + $Paths.IconFile)

    Write-Stage -Percent 35 -Status $script:Messages.PreparingJava
    $javaExecutable = Install-JavaRuntime -Paths $Paths

    Write-Stage -Percent 80 -Status $script:Messages.WritingLauncher
    Create-WindowsLauncherScript -Paths $Paths

    Write-Stage -Percent 90 -Status $script:Messages.CreatingShortcut
    Create-WindowsShortcut -Paths $Paths -ShortcutPath $Paths.Shortcut -JavaExecutable $javaExecutable

    Write-Stage -Percent 100 -Status $script:Messages.Done -Color Green
    Complete-ProgressLine
    Write-Log -Level 'INFO' -Message 'Installation completed successfully.'
    Show-PostInstallMenu -Paths $Paths -JavaExecutable $javaExecutable
}

function Invoke-LiteLauncherInstaller {
    Write-InstallerBanner
    $paths = Get-InstallerPaths
    $action = Get-InstallerAction -Paths $paths

    if ($action -eq 'Exit') {
        Write-Host ''
        Write-Host ('  ' + $script:Messages.NoChanges) -ForegroundColor DarkGray
        Start-Sleep -Milliseconds 350
        return
    }

    Initialize-OperationLog -Paths $paths -Action $action

    if ($action -eq 'Uninstall') {
        Uninstall-LiteLauncher -Paths $paths
        return
    }

    if ($action -eq 'CreateLocalShortcut') {
        Create-LocalShortcutOnly -Paths $paths
        return
    }

    if ($action -eq 'OpenShortcutFolder') {
        Open-ShortcutFolderOnly -Paths $paths
        return
    }

    Assert-X64Windows
    Enable-Tls12

    if ($action -eq 'Reinstall') {
        Prepare-FullReinstallation -Paths $paths
    }

    Install-LiteLauncher -Paths $paths
}

$script:BootstrapBase64 = @'
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
50nBj3RHqykKCq4x9NxflEqvKIx9lOY6yL96GunuSZ/FNK0O0MhobIgug92kSR0y9FR94c0IoBOCZlpRCHPI0tiMHIySgeskE6Dx8C0od6DN3kJrMPgLNtVh
Gd16LLh9CTvuYNdsbBm7J+4gPNsfu40oEBxcxt7EwBIOrHrtgULPboK4jXxvx1bsQAi9OEj3nkMY8JFEi95wAfM+wt0wsUBYQoTUgk2WDhLOPC6ivl2VtC7h
/IusSs2xKEE8ePo2huhCdQvPRJcwegP7J2LB5/qXoPf/hpPXsJWmp37HJjksYeY6VDm7gYbguUSUML98049HB/kcJ7sZH5dKz51ooyrpoFUnIeoidD3oo8vN
HrqAhOl2EKH/AWLlcxwlpB4KZKELg1jEJT/qY/454M+KZ7vs+wrQJ3T8H/TRky4Zp/8m5QCu+MF7Fa/R2ESGruINAvAWrTpp/1PaeRvv0E/O3seH9PvGhxkg
KY5vKdhXafyOxjdo9v3RbvyAnyipW/4DUEsHCEvNvLCKBQAAswwAAFBLAwQUAAgICACndfRcAAAAAAAAAAAAAAAAMQAAAG5ldC9saXRlbGF1bmNoZXIvYm9v
dHN0cmFwL0Jvb3RzdHJhcEJhY2tlbmQuY2xhc3O1WAl0XNdZ/n5pNG8sPdvyWJIzXieOl9FotZ26tuQltuQ4SkZSYsmyZTttn2ae5GfN1pk3dpJCymFrC7SG
tpzGZjm0TesWSsBQyxtpIIVAmgJlpyyBsrWUpVBoS6A0fPe+mdHiGVntOcieeffe999//7/73/nMt259CsARmaxHDWoN+EzUwS9oPGedt7qSVnqqa3jinB13
Bf59TtpxDwhqI61jyxDAMgP1JhpgCtrSttuVdFw7aRXS8bN2rmsik3Hzbs7Kdh0ujQ5b8Wk7nSCDZGZKsD125yaPYnZLLDPVa2CFYEdV2kTmQjqZsRJd/cXB
iJ0778TteixHo7JmlSAaWaKs1jEDqwU7FzEnVlwatNLOpJ13y+KCaFReaTGxBvcIlqWKBHnB/RXE340h7V5b0e47Ng6k866VTJYVCWG9snuD4PFF7K7mt94l
u0pZu8lEGPfS2hIxrd21FGvnK01j7xN0V5Wss/Fhfh0rpF0nZZdt3Yz1So1tJrYjIjDPzdJQk53VbanCsbee3BrVV5uJdnQIVhayCcu1D6UTnuqCrRW8WnC6
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
AAAAAACMAAAAYXNzZXRzL1BLAQIKAAoAAAgAAKd19FwAAAAAAAAAAAAAAAAMAAAAAAAAAAAAAAAAALEAAABhc3NldHMvZm9udC9QSwECFAAUAAgICAAAePBc
SeTo8rcQAAC8EAAAHAAAAAAAAAAAAAAAAADbAAAAYXNzZXRzL2ZvbnQvcGl4ZWxzX2F0bGFzLnBuZ1BLAQIKAAoAAAgAAKd19FwAAAAAAAAAAAAAAAANAAAA
AAAAAAAAAAAAANwRAABhc3NldHMvbGlnaHQvUEsBAhQAFAAICAgAAHjwXKgHNziwAQAADgIAABUAAAAAAAAAAAAAAAAABxIAAGFzc2V0cy9saWdodC9sb2dv
LnBuZ1BLAQIKAAoAAAgAAKd19FwAAAAAAAAAAAAAAAAJAAAAAAAAAAAAAAAAAPoTAABNRVRBLUlORi9QSwECCgAKAAAIAACndfRcAAAAAAAAAAAAAAAABAAA
AAAAAAAAAAAAAAAhFAAAbmV0L1BLAQIKAAoAAAgAAKd19FwAAAAAAAAAAAAAAAARAAAAAAAAAAAAAAAAAEMUAABuZXQvbGl0ZWxhdW5jaGVyL1BLAQIKAAoA
AAgAAKd19FwAAAAAAAAAAAAAAAAZAAAAAAAAAAAAAAAAAHIUAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvUEsBAhQAFAAICAgAp3X0XNQEGp/qCAAAJRIA
ACsAAAAAAAAAAAAAAAAAqRQAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9Cb290c3RyYXBMb2cuY2xhc3NQSwECCgAKAAAIAACndfRcAAAAAAAAAAAAAAAA
IgAAAAAAAAAAAAAAAADsHQAAbmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL2Rvd25sb2FkL1BLAQIUABQACAgIAKd19FxaPljQtgEAAFEDAAA5AAAAAAAAAAAA
AAAAACweAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvZG93bmxvYWQvRG93bmxvYWRFeGNlcHRpb24uY2xhc3NQSwECFAAUAAgICACndfRcsPSLcf0DAACU
CQAANAAAAAAAAAAAAAAAAABJIAAAbmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL2Rvd25sb2FkL0Rvd25sb2FkRmlsZS5jbGFzc1BLAQIUABQACAgIAKd19Fxy
cZjOzAAAABkBAAA4AAAAAAAAAAAAAAAAAKgkAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvZG93bmxvYWQvRG93bmxvYWRQcm9ncmVzcy5jbGFzc1BLAQIU
ABQACAgIAKd19Fy+9o2LqQMAAHsJAABJAAAAAAAAAAAAAAAAANolAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvZG93bmxvYWQvRG93bmxvYWRTZXJ2aWNl
JFZhbGlkYXRpb25TdW1tYXJ5LmNsYXNzUEsBAhQAFAAICAgAp3X0XAXbgMucKAAA8VwAADcAAAAAAAAAAAAAAAAA+ikAAG5ldC9saXRlbGF1bmNoZXIvYmFj
a2VuZC9kb3dubG9hZC9Eb3dubG9hZFNlcnZpY2UuY2xhc3NQSwECCgAKAAAIAACndfRcAAAAAAAAAAAAAAAAHgAAAAAAAAAAAAAAAAD7UgAAbmV0L2xpdGVs
YXVuY2hlci9iYWNrZW5kL2phdmEvUEsBAhQAFAAICAgApnX0XKg4z784CAAA+BEAADYAAAAAAAAAAAAAAAAAN1MAAG5ldC9saXRlbGF1bmNoZXIvYmFja2Vu
ZC9qYXZhL0phdmFSdW50aW1lTG9jYXRvci5jbGFzc1BLAQIUABQACAgIAKd19FxzWE4jIwwAAPAXAABJAAAAAAAAAAAAAAAAANNbAABuZXQvbGl0ZWxhdW5j
aGVyL2JhY2tlbmQvamF2YS9KYXZhUnVudGltZU1hbmlmZXN0U2VydmljZSRKc29uUGFyc2VyLmNsYXNzUEsBAhQAFAAICAgAp3X0XFmnNV1SEAAA9SAAAD4A
AAAAAAAAAAAAAAAAbWgAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9qYXZhL0phdmFSdW50aW1lTWFuaWZlc3RTZXJ2aWNlLmNsYXNzUEsBAhQAFAAICAgA
p3X0XEydrnCAAwAAFwcAADYAAAAAAAAAAAAAAAAAK3kAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9qYXZhL0phdmFSdW50aW1lUGFja2FnZS5jbGFzc1BL
AQIUABQACAgIAKd19FyoJq69MBoAAEg5AAA2AAAAAAAAAAAAAAAAAA99AABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvamF2YS9KYXZhUnVudGltZVNlcnZp
Y2UuY2xhc3NQSwECCgAKAAAIAACndfRcAAAAAAAAAAAAAAAAIgAAAAAAAAAAAAAAAACjlwAAbmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL3BsYXRmb3JtL1BL
AQIUABQACAgIAKd19FyjY4RDLAQAALYIAAA1AAAAAAAAAAAAAAAAAOOXAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvcGxhdGZvcm0vTGF1bmNoZXJQYXRo
cy5jbGFzc1BLAQIUABQACAgIAKd19FxMZ9MwrQQAAO8JAAA3AAAAAAAAAAAAAAAAAHKcAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvcGxhdGZvcm0vT3Bl
cmF0aW5nU3lzdGVtLmNsYXNzUEsBAhQAFAAICAgAp3X0XMjdOUF3CQAAFRMAAC8AAAAAAAAAAAAAAAAAhKEAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9w
bGF0Zm9ybS9PU1V0aWxzLmNsYXNzUEsBAgoACgAACAAAp3X0XAAAAAAAAAAAAAAAABsAAAAAAAAAAAAAAAAAWKsAAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0
cmFwL1BLAQIUABQACAgIAKd19Fz4PCbClAIAAKQFAAA/AAAAAAAAAAAAAAAAAJGrAABuZXQvbGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9Cb290c3RyYXAkQm9v
dHN0cmFwU2NlbmUkU3RhZ2UuY2xhc3NQSwECFAAUAAgICACndfRcmNT2nf0IAACWEwAAOQAAAAAAAAAAAAAAAACSrgAAbmV0L2xpdGVsYXVuY2hlci9ib290
c3RyYXAvQm9vdHN0cmFwJEJvb3RzdHJhcFNjZW5lLmNsYXNzUEsBAhQAFAAICAgAp3X0XLzV0QOwBAAA3goAADYAAAAAAAAAAAAAAAAA9rcAAG5ldC9saXRl
bGF1bmNoZXIvYm9vdHN0cmFwL0Jvb3RzdHJhcCRCb290c3RyYXBVaS5jbGFzc1BLAQIUABQACAgIAKd19FxLzbywigUAALMMAAAqAAAAAAAAAAAAAAAAAAq9
AABuZXQvbGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9Cb290c3RyYXAuY2xhc3NQSwECFAAUAAgICACndfRctegozFUNAAAFHgAAMQAAAAAAAAAAAAAAAADswgAA
bmV0L2xpdGVsYXVuY2hlci9ib290c3RyYXAvQm9vdHN0cmFwQmFja2VuZC5jbGFzc1BLAQIUABQACAgIAKd19FxZbCslOAEAAD4CAAAzAAAAAAAAAAAAAAAA
AKDQAABuZXQvbGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9Cb290c3RyYXBFeGNlcHRpb24uY2xhc3NQSwECFAAUAAgICACndfRciVhWDmENAAD8HwAANwAAAAAA
AAAAAAAAAAA50gAAbmV0L2xpdGVsYXVuY2hlci9ib290c3RyYXAvTGF1bmNoZXJJbnN0YWxsU2VydmljZS5jbGFzc1BLAQIUABQACAgIAKd19FzX9KwfdQwA
AA8bAAAxAAAAAAAAAAAAAAAAAP/fAABuZXQvbGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9MYXVuY2hlck1hbmlmZXN0LmNsYXNzUEsBAhQAFAAICAgAp3X0XAbo
vRGADgAA9h8AADgAAAAAAAAAAAAAAAAA0+wAAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0cmFwL0xhdW5jaGVyTWFuaWZlc3RTZXJ2aWNlLmNsYXNzUEsBAhQA
FAAICAgApnX0XKclgWK+AgAAmwYAAC0AAAAAAAAAAAAAAAAAufsAAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0cmFwL01hbmlmZXN0TG9hZC5jbGFzc1BLAQIK
AAoAAAgAAKd19FwAAAAAAAAAAAAAAAAUAAAAAAAAAAAAAAAAANL+AABuZXQvbGl0ZWxhdW5jaGVyL3VpL1BLAQIUABQACAgIAKd19FzxV09bwQYAAHkMAAAj
AAAAAAAAAAAAAAAAAAT/AABuZXQvbGl0ZWxhdW5jaGVyL3VpL0FwcFdpbmRvdy5jbGFzc1BLAQIUABQACAgIAKZ19FyeGC+ZmAQAALsIAAAhAAAAAAAAAAAA
AAAAABYGAQBuZXQvbGl0ZWxhdW5jaGVyL3VpL0hvdHNwb3QuY2xhc3NQSwECFAAUAAgICACmdfRcMNaL4FYDAABZBgAAJQAAAAAAAAAAAAAAAAD9CgEAbmV0
L2xpdGVsYXVuY2hlci91aS9Nb3VzZUN1cnNvci5jbGFzc1BLAQIUABQACAgIAKZ19Fz6ENkzXAQAAJYJAAAkAAAAAAAAAAAAAAAAAKYOAQBuZXQvbGl0ZWxh
dW5jaGVyL3VpL01vdXNlU3RhdGUuY2xhc3NQSwECFAAUAAgICACndfRcnmvem24CAAAhBAAAIQAAAAAAAAAAAAAAAABUEwEAbmV0L2xpdGVsYXVuY2hlci91
aS9QYWxldHRlLmNsYXNzUEsBAhQAFAAICAgAp3X0XHmvYqULAQAAzwEAAC4AAAAAAAAAAAAAAAAAERYBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxCdXR0
b24kUmVuZGVyZXIuY2xhc3NQSwECFAAUAAgICACndfRc2N5mRq4CAAA8BQAAKwAAAAAAAAAAAAAAAAB4FwEAbmV0L2xpdGVsYXVuY2hlci91aS9QaXhlbEJ1
dHRvbiRTdGF0ZS5jbGFzc1BLAQIUABQACAgIAKd19FxTPp9oXwYAAMAMAAAlAAAAAAAAAAAAAAAAAH8aAQBuZXQvbGl0ZWxhdW5jaGVyL3VpL1BpeGVsQnV0
dG9uLmNsYXNzUEsBAhQAFAAICAgApnX0XK4rHaECBAAAQQgAADIAAAAAAAAAAAAAAAAAMSEBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxDYW52YXMkSW5w
dXRIYW5kbGVyLmNsYXNzUEsBAhQAFAAICAgApnX0XE1+D/WqCAAALBEAACUAAAAAAAAAAAAAAAAAkyUBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxDYW52
YXMuY2xhc3NQSwECFAAUAAgICACmdfRc/8QmNpkPAAB9HgAAJwAAAAAAAAAAAAAAAACQLgEAbmV0L2xpdGVsYXVuY2hlci91aS9QaXhlbEdyYXBoaWNzLmNs
YXNzUEsBAhQAFAAICAgAp3X0XLZJ+qdzCQAAQRYAACYAAAAAAAAAAAAAAAAAfj4BAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxQYWludGVyLmNsYXNzUEsB
AhQAFAAICAgApnX0XAKdmqCOAgAAcgUAACYAAAAAAAAAAAAAAAAARUgBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxTdXJmYWNlLmNsYXNzUEsBAhQAFAAI
CAgAp3X0XJMvP1SRAgAAEAUAAC0AAAAAAAAAAAAAAAAAJ0sBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxUZXh0JEFsaWdubWVudC5jbGFzc1BLAQIUABQA
CAgIAKd19FwWU9IGJAgAAOEPAAAjAAAAAAAAAAAAAAAAABNOAQBuZXQvbGl0ZWxhdW5jaGVyL3VpL1BpeGVsVGV4dC5jbGFzc1BLAQIUABQACAgIAKZ19Fxq
l11MdAIAADUEAAAlAAAAAAAAAAAAAAAAAIhWAQBuZXQvbGl0ZWxhdW5jaGVyL3VpL1Byb2dyZXNzQmFyLmNsYXNzUEsBAhQAFAAICAgApnX0XBWe8NXAAAAA
8QAAACYAAAAAAAAAAAAAAAAAT1kBAG5ldC9saXRlbGF1bmNoZXIvdWkvVGFza1Byb2dyZXNzLmNsYXNzUEsBAgoACgAACAAAp3X0XAAAAAAAAAAAAAAAABkA
AAAAAAAAAAAAAAAAY1oBAG5ldC9saXRlbGF1bmNoZXIvdWkvdGV4dC9QSwECFAAUAAgICACndfRc0VlGJFcDAADVBQAAKgAAAAAAAAAAAAAAAACaWgEAbmV0
L2xpdGVsYXVuY2hlci91aS90ZXh0L0dseXBoTGF5b3V0LmNsYXNzUEsBAhQAFAAICAgApnX0XPxSG4mRAwAAvAgAAC0AAAAAAAAAAAAAAAAASV4BAG5ldC9s
aXRlbGF1bmNoZXIvdWkvdGV4dC9UZXh0Rm9udCRHbHlwaC5jbGFzc1BLAQIUABQACAgIAKZ19Fxq6CYzij0AAAF+AAAnAAAAAAAAAAAAAAAAADViAQBuZXQv
bGl0ZWxhdW5jaGVyL3VpL3RleHQvVGV4dEZvbnQuY2xhc3NQSwECFAAUAAgICACmdfRcMny079MHAAC6DgAALQAAAAAAAAAAAAAAAAAUoAEAbmV0L2xpdGVs
YXVuY2hlci91aS90ZXh0L1RleHRSYXN0ZXJpemVyLmNsYXNzUEsBAhQAFAAICAgAp3X0XEVxQRVrBwAAehAAACYAAAAAAAAAAAAAAAAAQqgBAG5ldC9saXRl
bGF1bmNoZXIvdWkvV2luZG93Q2hyb21lLmNsYXNzUEsFBgAAAAA+AD4A7RQAAAGwAQAAAA==
'@

$script:IconBase64 = @'
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
'@

try {
    Invoke-LiteLauncherInstaller
    exit 0
} catch {
    Show-InstallationFailure -Exception $_.Exception
    exit 10
}
