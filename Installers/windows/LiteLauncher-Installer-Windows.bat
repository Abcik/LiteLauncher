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
nFgowUc94H27mV3jTMrTmyd9vt30kUs3hDRndEuwkfupmiN/0bqYlkzriuePqTbpsoP1wDwWnCJGynJC9vXTc2I00oTjnV+/aXEK/Js/WW/Nl7zDyZBXn9qS
v19D1aA3cNV8D/y/0P+vWlSZP814uJnkWOF4meHPARuY6VfqPkr6H1BLBwhJ5OjytxAAALwQAABQSwMECgAACAAAd5hzXQAAAAAAAAAAAAAAAA0AAABhc3Nl
dHMvbGlnaHQvUEsDBBQACAgIAHeYc10AAAAAAAAAAAAAAAAVAAAAYXNzZXRzL2xpZ2h0L2xvZ28ucG5n6wzwc+flkuJiYGDg9fRwCQLSQiDMwQwkEwXqUoEU
Y3GQuxPDunMyL4EclnRHX0cGho393H8SWYF8xQCfENfZc2/8+v/z3+9//+58v/z/ZEntmZ9//x9/8//3r//3z/0HqloqrxcApLhLgvyC/8MBg5c/4yegMGeB
R2QxUFYYhBkZZs2RAApKlLhGlATnp5WUJxalMgQkZuaV6Pm5hiiY6hnqWUhnvloBVLMtNcIzzdNTi4EDyGFlkGJkZWAEsryAWBrKBvlIg5EZzGYCYkNGJgYB
IB0FxJntLGDxLAYI8JRkZHjBjKDRLGUACk1gZ2ABqjQwMjZgXAAxFYRZF0BMmgI1iQnIYwKrDLK0ALKgugwNDECyue077jbsWzUbyKz0dHEM0QgOPXeb74AC
B3NAG7Ps//8Hs7vTIuV75hVc9DJMvV96Y6nJJ4PvhnksN1IY3FeET9CJ3iXiNqsyoyPnrI/EPZUbaibldrM+u/e/iBA+ZZb4aFlfVxu7XaFPduz/KP7e7Ivz
VSSfFyq0hGxsWdPSodXgEzCrgyc/VF8xn+37gy8KD8Eed/VzWeeU0AQAUEsHCKgHNziwAQAADgIAAFBLAwQKAAAIAAB2mHNdAAAAAAAAAAAAAAAABAAAAG5l
dC9QSwMECgAACAAAdphzXQAAAAAAAAAAAAAAABEAAABuZXQvbGl0ZWxhdW5jaGVyL1BLAwQKAAAIAAB2mHNdAAAAAAAAAAAAAAAAGQAAAG5ldC9saXRlbGF1
bmNoZXIvYmFja2VuZC9QSwMEFAAICAgAdphzXQAAAAAAAAAAAAAAACsAAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvQm9vdHN0cmFwTG9nLmNsYXNzjVf7
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
X9OgafUtRs1lnCZ0v9Tqr/evOjyNJ4mcaTwdem4aZ+jMmdDXOTRfQ5BpefEsAqFXWHR+nyG2OA61XMPyen/LDL6pVkp7vX8a316EuO+VIe4c/DTnI3CWtdZw
rq/RyAnwcnyGMQfwPDED/r2i53LktJP6IOoZ/WZG3449fNt24hAvqFHEKL+fGvbhOXTjVRzQeUkhcJ3bNSXk8HeKpTN40RFCp4mg09fViatAVwM31l4nCP0L
6Bp/Iqbxx3+o1aVbREZHEX+bLuN1ZvaHcXaHN4abLuLH03izqXQqp4G5475Co6+HsfWyPyS4+rmO+wrUv/nbCLo/kPMdPf5Kj1f1+Bs9/laPv8ffNFwF/o5/
4t+o/z9QSwcIBgxK2R4IAAAlEAAAUEsDBAoAAAgAAHaYc10AAAAAAAAAAAAAAAAiAAAAbmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL2Rvd25sb2FkL1BLAwQU
AAgICAB2mHNdAAAAAAAAAAAAAAAAOQAAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9kb3dubG9hZC9Eb3dubG9hZEV4Y2VwdGlvbi5jbGFzc42QTU/CQBCG
36WlhVK+hRs3D7QYG+MR9eLHiRgTCAdupd1Ateyapog/youJhsSDP8AfZZyWRkjk4GVm9t153tmdr++PTwDnaBvIQdGhmshDYzgVPHbCIOahuxTenEfO1PUe
uPAdX65EKF3fucqK62ePP8aBFAzaWSCC+IKh1x3cu0+uE7pi5gzjKBCz/o4ymkdy5U5D3p9YYwMFFHUYyeQSQ3PbtuNs/9fQGhfpJ2UTFVQZ6p4UgnuJx10k
6X7BwCYMrT121phBvZQ+Z6gOAsFvl4spj0aJK4PStQgzhnIZefwmSKT2nwUcJ544ofkFWipDI3kEVSrVedQo1ul0SOcc5YLdY8oa7DXrBYyNDh3FlCjtEMqG
ONpHmESUU0LLiCPqV5MbItbQ7c47zC1VSd1q1F2nGQ1SK2hmZIcyo5y332C+/CJaKrYoHlDOofUDUEsHCAPCDv9PAQAAOgIAAFBLAwQUAAgICAB2mHNdAAAA
AAAAAAAAAAAANAAAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9kb3dubG9hZC9Eb3dubG9hZEZpbGUuY2xhc3OVVF1z20QUPZvIXttRcOLEJmpIXKANttxE
LeUzbkvTpKUJThqaTmHgBdleYjWyZCS5DDzxb3iBGTqDh5nMwCsz/CjgriS7zlhkyMPdj6uzZ+/VPXv/+vv0DwC3cZTDFKY5FBUppBnmnpnPTcM2nWPjsWi5
XpshfctyrOAOw3Sl+jSDDAOyyGGGQ1Uxi1cYNhwRGLYVCNvsO62O8Iym2ToRTttou986tmu2jZ148cCyBTH1PZuh0Hh52VHgWc5xXRLPqZhHgUHpmUGHYTFC
OZZrfE2HjUPyhrhFFUV5+zzd2Drx+90t+9j1rKDTlV9fVbEkv2aGX6XzkoplvEbUvvU9hcH2pHNVRVkiU7bZFHaBfsvrKt7Am3Q2cKO4GHYqjYslWa9OZie5
r6pYw1vE3TH9zrbbpjBuXpx7V1JVVeioUYHEN33T9hnuX5hoLMZHzWeiFdSrXzB8VZmMPakK/4Ua9+xNuqpPqQJR6vmG5YiDfrcpvCdmU0pj9iigiPfNXrjn
uHFGkxEDx01Sz2RABN0XQcdtH5qe2RWB8OinLFYSKhGKeZehWEn+A6XhobMJh8f2GHJHbt9riUjM8+N/dEOeopIMH8/cPdcN/MAze1FkfgYf0YMh/delvOsT
2h156lKj9VCTeYYsWZ5sgaxEppGt5NP4OIeH2OXYU/EJGgzll+l4fSewuiJOK76emJrDiBhejKdvOc/dE2FEwIem07aFf6Xhuif93vmVjg8++a4ndoTf8qxe
4HrjgG3b9P0Eji/Pv/3ME4qLw6DuOo7wQkrhczxiuPq/cuD4lGH1fCgVLgLjBmRjBNIoyB5Hq4LsSuFcjPdL8UwtJZzL4T4HaiW4RqfXafc7ppGh+XP9N7Da
n8gU+I9Qao314XL9QFPidUpTNhWNj3Z8k+u1AbL6tQHy+voAC7qmDFDSS6kBNF3jA6y8IO4pbNC4Ck4jdXK6L08d+hJFUsEM3iPfZ1Bh0FcDmX8oHc6R47jO
8XY4XlfIgLs0ZuWQ26JhRg54hw5pacz+AMyTFcmWyJbJytJHffJdyldmWqaZyQj0U1wGfqElCwNLh+4UgdcomEnwFeCnBLCO92PwZcpQ1iKj104ppQQ0tY1R
HCsxdUr/FdnJKOi94oMEaD4JWkxkXUiCLiVCS0nQZXw4gk6NoNrPCdByIuvKJKuCzVAJddyieY38kWojzUaKjfQaqTXS6m2yEjE8IM3kcAd3sYV72MYO7mM/
1vIBDvEY2r9QSwcITKkB6qUDAAAyCAAAUEsDBBQACAgIAHaYc10AAAAAAAAAAAAAAAA4AAAAbmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL2Rvd25sb2FkL0Rv
d25sb2FkUHJvZ3Jlc3MuY2xhc3NlT7FKBEEMfTl1V8/GxsLK9mwcsD0shONAEBQPrs/OxnXOMSOzWf03Cz/AjxJnRVA0RfJ4yXtJ3j9e3wCcY7/GhHCmYi4G
k8iD+nvJrmH/INq6Nr1oTNy6xTe4yanL0vc1tgkHG35mF1k7d91sxBuhGp5aNiHMZourn/bKctBu/p85WROmqzRkL8sQi+7w76bTUUM4uh3UwqOsQx+aKBeq
ydhC0p5w/Mt3WT4YaY6XapLv2Mu8IhC2MAaVs3dQFTRB/ZV3sVdqVSamwCdQSwcIQXQ/9swAAAAZAQAAUEsDBBQACAgIAHaYc10AAAAAAAAAAAAAAABJAAAA
bmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL2Rvd25sb2FkL0Rvd25sb2FkU2VydmljZSRWYWxpZGF0aW9uU3VtbWFyeS5jbGFzc61Va08bRxQ9A8ZryFJjh7gJ
lEAoSWzTsk1p+lpKGiApOM5DdYTU9NPYO4KF9Q7dnSVC/Uut1EhFlfiQH5AfFeXO7lITvEKl4cP6zuPMPTP33Hv95u3RawA/4NEIBjBoIGdiCHmGsR2+zy2P
+1vWz6IjA4chv+T6rlpmGKzWNodRwLCBEROXYDKs+kJZnquExyO/sy0Cq807u8J3LEe+9D3JHWstHbREsO92xNwm91yHK1f6rajb5cEBg9F1w9D1t4i+GfNH
yvWsphsqW/N9ZKKIMYYRJRX3Vg6UCBlYQ2+VTVzWW8OO9EW8o1evmKjEB/Y110PXE2GZXnvVxDVMMBSUbKkg5vul2ryAF9i1Zi9siWtbE35iYgrXiXCbh9ur
0hEM6xdEuKH9z5i4gVlSSPwWcY+C8uJivJ94zdP2jugou/aCYtxyt3yuooCesXJKp6Vz8Go97GWboVI9rXaj0ahtMuSSWBWbri+eRN22CJ7ztkcrY4+F2pbO
Mx7wrlAiCOOAfvBVUtrxaoaOcdpvMFypZkelfHyo9wqGtb7F/xUgYm5QFrdkFHSEXqU7nlJvQdNQhj0RoVqXoTLwFcOdcycB5dBxuY+tSKlCFfC9JNhhAd8y
3E5L1O7VoP1vzdm9OitSNdM3Sl+JvvFiHssjuIcfDdw3sYJVhuleHIPIV25XpPFM+SjT2sdXYHh1Mu6uvy93hZUA17nvEONcU8rdaM/ul67/4PODPbEmwk7g
7ikZnASsejwMM3z8ejb7e5WfZgWDueH7Iohd6lZV6qswAz8x3PxP7zJAyXf9bCipl4DxBXW/AeoMOZR1gwbDpzQbwBzNqYfSuKwbZmwrsdX4IdzEIG7RbJFs
gexk/R+w+vwhjPrMIUbrldwhSvVK/hDjr2ib8oF+82TBfkeVxqUc8YE4QP5BvpEy11CirlsnqPY/TZYOkdsjfAz81eerRB1zPgM8CfyRAb6Bz1LwDHHplxfq
80d0MgPN6IafE0ajp1LXQ/W/YfTfIrn6As2KsFKCKVo5PjL6Z4b3y5nQUha0kgkd74cOkaL6KnfwZWwXcZfsLHKn1E20TZRNdP061pjRX3yeFP0G3+F72FjC
Wrw+QGuLeIAJPMQ6Grj2DlBLBwi5cQ+xSwMAABEIAABQSwMEFAAICAgAdphzXQAAAAAAAAAAAAAAADcAAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvZG93
bmxvYWQvRG93bmxvYWRTZXJ2aWNlLmNsYXNz7Xx5fFTl1f85T2ZyJ5ObbbLABYUoiGGSENxABlAgBAgmgCSALIpDcgkjyUyYhUWtVavWpa212iqx1eIWrVjB
agiggtWitatLtVqX1qVaX7vY6vtWi+b3Pc+dycaA4Mf398/vxye59z7bec5znrN8z3NvePqzXY8S0VT1tpcUZRjkMslNmUz55wXXB6tag+GWqvmrzrOb4kyZ
U0LhUPw0poyysYuzyENZBnlNyiaT6YSwHa9qDcXt1mAi3LTGjlatCjattcPNVc2RDeHWSLC5ambyocGOrg812SDTGmlhOr7uoENnRCLxWDwabK+LtEz2YsJc
k/IoH5wkwqF1CZA4tqxO85mIh1qr6kKx+OSxgyuyyUeFBhWZVEwlTLkDm5mMUKymrT2+SS9rmcwy1CSLhjEVNbXawXCiPcV4o93WHmMqPHDOxTLsKJOOphFM
nvXB1lBzMA72lqRh74gFNXqxQy8UCTck2tqC0U2QRSkdY9CxJo2i0UzVXwJNCKItFIuFwtgSX9kBUpQZx5h0PJUxeeOReLB1xqa4HdNCmyuNfpPKpTGrORK2
dZsIvtKkcVTF5IqFzrd151rpfIJJJ2pKWlSzQq12zActPNmkU2gC05C24Fq7OhJuCsaXhOJr8BSLB8NxzDakrHZubZI5rZsN8ShY1spxqkmTKIC5QuHVEabi
sgO7jV3soSnQgeo1dtNaVJSulqll8GkmnU7ToFlRuz0ShVrUlx3BTi2IRlqidiw2eS7+pZtWpphhUrXIIi81vHFN1A42Y+VMNbLyWUwlZbXplmfQHKYxfVvS
BNkkolE7HK8KxiNtoaaq6fpWFwm3eGkunWFQnUn1NC+l75pYfTC+BnvQFtzI5C6bO1c2bjYtEIM/E2Irmyt8zhYn0Mg09vNng3mKgXipgTK8tITOMmipScto
OdOItKNrNtpNiXgkio30he0Ns0Ib7aQQFoAU5iyr7a94aUYm1RcCOZtp3CG7Vkfa2lttreHOIC+toJWy2HMH+Y00g7FlBq2Crff1mh6NBjeJLXgpSBmi3LZJ
qwnm4oGORCGZqOxf/wXUJuvhg0J0nkFrTWoVH+Q7sAvMb00wNs/eGJe+YZMi1I49CaMCjqisv0447hgiwKBxh6+jYmU++Pi4SQlaD9pNwdZW5uuPRM9T4k8v
vHTKMfkIqAuHh0NatPx/s592eIc97gt4iYMoeTU2JLgKMhBN3WjSJjofDimWWNUWghKcdhCV7R11EKqzEvFE1NZh8EKTvkIXwQUEm5sHOcikUkkAXEEXm3QJ
XQoVicMPM40s+zzSl9HlBl1h0tdFaYcfoi/mbrHjBl2VchF68towrCCaaI/bzTUbm+x2MVovXUPfMOibJn2Lrh0ARxyPwZSTJJ0qD7QSp3aykLnOpO8IpMkK
peYRl3WDSd+l74GhmB0X77fM8dI3mbRZgn9mUzDcZLcadDPTSYe/x738e+gHTNaisGxNaTxSmurphJxxXuqgW8UZ/ZDJnyZSDVxJZIOzw3BKtwExHcJ1YeZ+
IvTQHSbdSXdpxLQBQIqpoiw96bS1QuFuk+6hHzEVNA9eIlPdwYh9AYFBjbbSfQb92KT7BdSMPpxAwJQdW5OIC9V5kQ1ucv55sugn9KBBD5nURTuYjk5LqjHU
Zi8Ki2kZDTXV8+fNbICqp1f0VFfN406TdtFuqGRwQzAUb7SjbaFw0JHHqLK5n0Ng7DIPPYI4kVp+aTv8ZGksHmlvt5uhFetoj0l76TFoZLsO170RJRyKVInu
VC1APbz/40xDBzbUhcJr5yd3/me0z6AnTXqKfp6KYr39NN6C/YRiC+2WRGswKhVMJ5alm2h53cEm0c5iHf1CIF6ZzPgrefq1gLR0hARtrKPfmvQMPYuY2SQQ
LJZoG2y3vYDuefqdQS+Y9KJEzPzBHTRun4GKtUL1JZNeFqoFKarTW1siUUDHNtHgV0x6lV6DRBFhIdFAWvbSoLb0XP3RpD/RG2DJXpcItsZqW8KRqF0djNkH
gZzLDHortQW6oVfjDfozhNU/7oTX2s1zwGR9sN1Lbzso4y8mvSf2oCKrs+l9+qtBfzPp76IhvgOXAScHdtqAqs+3s+kD+qdB/zLpQ/oI2903Ub04guz2RLx2
9fRVMegn06lpgkGa8HBglczyPyb9mz6GkwGcT4hqDR0QMaojra3oKioj2Ok/4vb2wzmWpe/j+OHPTOph0tuscxV44SNBKhpLaAVlZXKGKEdGItoKt8tumT+A
NIQNPLFHbGxw9AeM12yw1+RsRn7riSP5cyTsT6/e6SqxYZxrcp7sVhYC34KgeAODC5iOG9g9GIe6rErEHeucnirBrLjQ5CIuFuVGRIvbM0NRCAraLYJeeFg2
exDi6XmWKYeYPJQtJA/NNiC0Xbu6ZiOkEjuoaUvazMNNPoqPhqikYV6wDQYx88i37EAL8lEGj0R+xKVMk8pqDxkrD2W6Hj7W5FE8WpaVwrNogevjjiPm8+Au
5P8+dD1IzPlysOqRQP0vb10pkjoj5TEm64OHvFAYNtCG3tPDzbPtuLSNFdRZJrtbbnKF6J8bcNiGpT/P40yu4vHwIdovzV+NHDod5k2nci4+UVTupEGe8Ug1
7hSTJ/BEOF/HkM5MhOx466aD2pF2OZNMDvBkxMUofKJsEAwWzieePHBpOEge8MX1R6Zt4KkiyBIfPOJI4WKaydMFnLqBmCNRqak2eSbXYBcE9sywVyPsLbTj
cnLkQt6+2AeIfaKX53CtwXNNPoPrmExnkdC6RQtrBVNrD9aXfqeRYLLzZKE0z+T5vAA8xCOLFoJcftmATnXotJAbDG40eREvHjgduudG2u1wdSQcdgIL07BB
4/vaJht8Ftp7G+fE4+0DOnh5CS8zebkIoAAZQ7JFcF0kEZfWs00+R1pz0boQqUe/pnNNDkqqMQxNtfosq8mehc2NbFhoN2t3HvNwE5N3UcyOVk5vwTZ52E6u
Z9x6OxqThIIBelxS4+UQn2fwWpNbRUH7Y6NNMagKYrvEmiiWH41v8pHBYdHlyCD1P4Tmgud1JkdZDmr0ctYhrPdSRAZyeDaxWAglTF6vj71ahFCsPRKO2dWR
ZlvaNpq8STY4E20Qtw9aNlLqLzT5K5KxeZtDsSZH0gZ/NYWiYDO183tRlI+y+BJZ36XCV5rwkHaBF7FblP5yk6/gr0suGWlrR3CGX2mQdNBdtmwZohpYucrk
q8X3FLXoTY9jb+rscEt8jXPWNpu/YfI3+VuI70iqHbfknKXNldHfNvk6/g6UokV2HogLDNjBNnEASbZkMX0N0MMbDsD186HGvbiev2fyjXwT5Bm2N8xPxPvR
nH1YYKCP2uQ+FvoTmpwB2EVevpm/b/APTL6Fb+0v+f6rcDmZt7ts+YyxtV7ewrcZfLvJd/CdEFg62ui7Abgcum1gTG2t4/E6Tb6b78GSnMNXSSTj8VYblBNf
4insEbhDrORek7eKCrqbWiMx2+AfD8Dwvbmul+/jbSZv5wegRNCAhkR7u/AgzA89SHa8WGR7r0DThxCy6Fl52iFQtJvp3INa1uelKwfWpD2JNnjXAQpWHWnf
5KhEFj/Mjxj8qMl7eC9T6cB+DfBczcFoc19/eJ+FNQvqplfXrKw5q7ahsXbebHj3us8bNlnm+anJj8sk2dMb59fXVq+sn7+4RhT8Zybv4yehXG2R9VCUpYdI
1w6t6f3mS4t0k8cF/KGXruFfmPxLOQZ3x1ptu93Lv+bfGPxbk59hySv7BLkwEY6HBNt6xZ+lCsUDcthk9WSh8rzJvxP3V4jWkD6og5o2QUMi0ZjBL6Zoa7WU
1A/ux8svIPXjl+lCg/8wIHGTRtjLqya/Jqgi30EV81v1q6lZkagMqvTBdv8kDvEN4Svd6wRJS94y+W1JSyRUzEoCdpH+Oya/K2cG2aFYKtPYJP3fM/m/dOIe
jzhUpPNfTf4b/x1b1apfpVUdKjPSC4hpL1CVdDQ+yuYPTP4n/0vOGm0hceLBI5QmsBpOQHa0akEUobMJaGJyNn/E/23w/5j8b/54QEI9YDZEGfAUt6OSRRya
4qE4Nvk/Ju/nT0Eu2CQhSCdhR36EnnZR8qYr0WZH9Zp6TEWK4SmBtGqCTcg8S9Py3Tto7GIZdq+PcjiMLVNuU2UqA8EHDinSut5uCK1q1UcnZYeAX4Py18tU
lkmbldc5IUVEvE+ZpspRudAEiYjBhBx8DBl89Jp0dYbKT6Gq/obRd0Kp8pQJjVaFpipSyHELQ7E+zAVDAZG2gzvSZR41BJl03yFrEisMOG8FpAKCkgNXZcHJ
qmFM5Yd74LoM/lIdxWT1AsMkd31nOGpE6nxRg8rw2jAmnhOJ9e+CxPXY3i5rAC01vkwCxH79jk2dius423sm3g/sGGp0UqAbNbFYDE6hoa5PoBxSY0ylsyVP
OBiOyCTaz7k2rtkLuKKQJV2hKpIvAJG3z1XjTFWlxsuLwVBY6mfOHDtT6pEA1avxHnUyfFRKe3vfmWZOW+p4T+l5qqkmqQDGRiOJcLOoysyxc32Uq6bADamp
6fHi3LR5V546XcZMk0OZuem6ZKsZqtpQM01Vo2bBXxw5PJDD8Hbn/XxZ2czDA7IeNQfLWq1foeXziR51BuQ1bhy06nlVb6p5ciDowawxeV8tlQtMdaa4/cxW
DRalqsFUjfKONyuWWBXThD0KSYu7Yc70yhO86iy11FDLTLVcrUi9Q4nZQCmh+KaqerAdbLFnhlq0k8xu6csjmE44uDWnJyBHPOocU61U58I5AEYOwHSzvjiK
7A9kZUGrsEl8p1c1K9tQq016T7UMeKU6x944S04r4/08yMAGTSZkqvPUWsiyObl+V9nY5TOEbJupwgp5TdZq3RvDRG8ARtOpVgHD30TpVENBBY47rI9O0J+n
edR6wcPYwY2m2iQ77YXoo3G91x51AaYfl1I2j/oKOBjXn3Blqk383FdNdbG6BDuYqpwftpmP/GTz//WjprEwnOzGmvoFKxsWzZpVi9zdd+CWAzqlPh1ZLAdA
4sfmL5lXN3/6zJXTG2V0YwMT12bAjWWA3gyQqlm4sqF2GdL4kur58+bVVDeubKytr5m/qHFlfW1dXW0D+h6PmFa4sAZEDmgK3KSPhw/vgyZZg0syYSQ+daGw
PS/RtsqONkrggTNJiakvrev3bvqLvHPGbDlA4k1r64PtyUm8vXEDFpHVEGoJB52XtJsGTznlSBX0tC/M5BVfwtyDP1/6AiSYrvpSGPkyPvcy1GYmtXwGU82X
wROEPOEAOuWHesU/xX9acqDVGmxb1RwcPTjzGD1ePt85vNx17DJYYopOkjeM5zMP52Tz/39X8rnflWj5L46EmqHE3oZIItpkO294iwapmv6ACo5vHqJqvS3e
B34gv9dJ1dvxNZHmmEfdCcjc+8Jav2INlOpvAKdyRan+hE8ekp8PTmWP6oQbbJAYKXixF4nLq24MdL56kxEaSU7lfGrPc6sfSXy811S3qFvz1X39PkLsW45H
3c80sj8WDTpH46VcdVKglEunVJZi9u0YnOpkN0t95WlS/xOmUb3LSI1cjazcbh5I4CHgkd6OToe+xh3ATHXYl7rUvqBuJ+rmNDYuKOW+2XYzmU5dKdCJ1DzC
NK6XrMxemvzAtLTJ+VZNpmnue70nkrkoL1PtFck8Zqrvqx/ks5WvnjjItzuL89x8cj5P9KinsABOj0I86mmklAzsWsrHedQvgWNZlvBrIFEuLeOxHvVbRL++
nEpemuudwtLAzLNe9Zx63lC/E5j+QuozDc1HKLw+stZOWr7z+easoD5AYLqpP8PJjo5+zQmGm6EGo+sikbWJ9kMfbA0Y2Lip3U7Tffmh3/AkSchXUw2Qz2Qs
6fde9ZJ62VB/MNUr6tVUrta/d512Vpg2uDq1oOy2/qVf/u8v7/AbnSkPPfZQIoHi1iLLjVa3BmMxeeVUcEBIMtSfUq+wP2/JhnpzwMde6boC2zud6QTykAIe
yyRLXaq+RqwuQ0mpC2mYulxd0Vv+OspX9pV5M8pX9Wu/GuVr+pW/QT75UB3Pwwmarr6Jlm+h5MdsMp/Xv4PYX/4QGdvlSFBdi2suCTKsJBeNoyyqQu231XWU
IeNcFaBmoeNn/vIuyplXuRPpBW0m13Z/ZRcNkcvwgMtyddHIgFtux03MlNvYiR4/+iL/ltJ4y62fd9FJRF000V9Rklni0S1Op8t9k7toqvRKUe+i6RO8/mJv
78iZeuRumr1UxnZT7Q6aH8jWZY88mvKoSUopR0o7aGEgdzc14GFRIK/Y202LA/m7acVSK38HnRMo2E1BNDUFfDIJAGig0CrcSWuEg5PkCUniXloXKLJ8VoHf
yrOKrGzLtHIqrdyKXRQTfjbspAsULens+XnGhMLiQsunmb2NVlsFXfTVnfQ1RiO4KOqmq7vo21Ye+Lveb/m66Mbd1LHU932raAfdsgdT9GvxW0VddHug2Cp+
jDo2U45VvJc6AiVWyR6/VdxFnXsuK+TOnhucIVb+TroXs1j5RbTtAXpgJ3W7ZM7sQKEzp7NPvochvE4KBIZ83rChBwyzhuzZTpk8jifxFLpS31fQFr5V3gjQ
lfQKX8f7oCEv8Fv8Dtr38VNShi6Kbv2TpuB6IvT8JMqnU6iYJtJRdCqNoknQyQDsYAquU+l0Oo1q8VyHpyaaRmGaQXGqpiuohr5Hs2gLzab7aQ5h4+kxOoOe
Qs/nqJ5eoQX0HzqTj6LFPJLO4nG0jCfRanC6nE+jFTyTzua5dA4voJXg+lw+h4LcTKv4PGriKDb9MrL5Goy7jkJ8I53Ht1Ir30cRrKyNH6Awd1M7P0Lr+HGK
YlUh/jX6vIA+b6HPO+jzF/T5O/p8iD6foM+nFFPfwYo/pPz9lG+Qr4fOJo8hPz5DzZArleLqchH10HopHtjEBs3u99Ng0FaDVugOIaJPaNJ+eIKlBl250qAt
n9LxuBp838eU+Qm5z0G1UY9iD5VSyRFRl2MuDCQWEj2UQwVHOFxdLwdw8Dsd6gatAd/Fcw/Lk/igF9Emnubc8n00FO7k0X1UILeMR+in3fREB7kztqL8dNYN
t1CRNHTTL6V8w+Zky2+66LkOPfQ30ozf33fTH7ro9c1kuDrJlbG1vmJPfcZWaBvRMFpGHfCCJ+v7KbRU36X+bHpT18td6uXuaOsIMnDdgNaNaNmEEefTcroA
PS6kc+gremcLyGOxNSJjGk8zqAOSebNv2XCxeVBzZ7lbsVg5kly5m96Gn3mnvvxJMrrp3W3l2tuIL9W+5uTKXlcDT7qv5z3xYo8+SZmdPa9X6Oed9F/iSHfS
PzK0sxHHVbGT/ptpB32yrdfWHO4vRWT5Gpi4jMrpcpoMC5pHX6cz6UrN/RByab38YD9lQpv20yiD1n1MxckN+x5WcDQIaefPGQgH2ai/Nukp67NmZE10Z000
nBUEsqys5BpWyVNyEV7Lm9zE7GP0Y3CGX+6fInaVGN7gRKPEnaUbYMzBie5Okm7M2Fx4O8cv7mDXngrLm3KvHbupdGnFMSXuEmMHZ/ateAKUlOgbWPE3yUvf
QularPw6xLHvwIdcD19xA82n79K5dCMl6Ca6CFt0CX1fS2IMZfbQUDKSOi6q7dL2tV8Mat3Iok/I8wnmGazTN+L3YnWJs8kZLWImENXFWl85K+BNBZxsDphS
uZNzgD0f4fxu9i1xTcgpzsm8nYt645Pl7eaSJf7iHARYHoZf3sUjnChZbnkrKi1XidsyLI+VZeVa2V18DDSCjzvLb3kqurisEr9SUVbilvEIm1P9SULCTuUu
PsGhJRI+eXsgz4/o1cWnWnkB08rq4ikdlAWGtlCmlbdnIBOnEVl5XXw6art4hkOgUwLpwUh4ESMlPB0OmdxAvvNo5e+5LIc7P3u/kyYFcvz9Bs7CQAw53crZ
k77Bj6tMZ5lyw/6fSrfQndhhub8Lm8adq7Bnd9Lr+v4uj5E70oATdLmY7uBq9L+Tp+m7lFdgnJRX9PqEu2gsrltgCrfBx92OXndBd+4GTroHM9yLuLWVFtJ9
0JwfUwQx6irahvn30B20HTM/QJ30E7Q8iKeH6HHsxUsw2tfR/jaM5V3Es/doJ30AgPMR7WYvPcyl9AiPoT1cQXvB5R6eiHuA7uZp9BS4/CnX0OO8lJ4Ahz9D
NNuHKPYkb6KntU6/RDk9YDdH6/S6Xi/tkps4cTjr9+WK0C3h5wLKPYyeqJKQ0LGfhuGqemj0kQx7Uw97M/PIhvF9n5AX0QxtHWdh+FH9fax83Jg0v2pA3UJs
6auiGt08u4vru/jMvbwkkGPlFByf38VLcQ/c1MUrrBzYyUorp5BXFXJzIa/u5jW7uA3mwe2iT/EJuf7iXKEjpQ3aeM7XxlOcW0BPbaFs3LliC5VJ+wWOn9rN
Fy0tzu0bcDHRDv6aYDonkP2AjrXMDEx82WYaKt2unJhXkie+0VtZArO45iypvDaQVwEPcX03fzeQX8ibH/YECoA3C7q4Y+kE383UIKYyZYBzLPZ1SIB4xcq3
CjKKfV38Qyu72He5UPRbWY5bcMqWW9xDpnYPlqeL7+rsucvK30djLEjnR500AnOhWJQsmoDDBcChfL9VsMfKk37g8yfSL1+KRcmiKcOExfthwv7dtG6piK5C
YnIyPj+Nigd3cBc8f8cAvy6+yf0I71ya8QDvbljqeoAfa+jmJwA/h0IQ0urqbZX67QijTwN3ToZ6zODZPBcYZS8v5iaUz+MIR3G/l5/gJ3tt9lJgH6JnYK/P
It4/R8fS81RGv4OtvgA8+SKtoN9TK6xlHb2MaPEqbPU1WPLrsMQ36GlQeQbW+Rr9mf5B79C/4TcU/YWz6T2gy/d5Mr3Bi3Fvwv0i+htvob/zvfQBP0EfgoN/
8s/pX/wKfaTt8Q7yfUqtBi/h0cd/RkMMvtngLYa6aT+NYONjOqVO47TcJNBap60gjW1omAUiyfEaovF9no9JpQjkHwGBvtEVy2FaRb2mBenJF5nJjPF01Al8
KZQ4M+MYrbPlxyREw7b3BmAndfw3On4MYX+iF51JvB/RFMRmwlk5WOjcJPQbWcRPDb881M1Pd1JxvZNqOJpRIUazXb7Igqc1kVc4EwzTWO4zysIemHBZiKVU
yEi0OUNPZogM6MqCARisWiexMu/cZNprdfOvuvi5KcPdt5Pb2DrcuJ0y8z1b8/O39i7Fh+HEmUDABuWyh0o4KzXDflTgJn8tnVzPL1AS4WzazS8s3cG/rx+E
6RrTYrp9PW84eE4ggQZ4z1b0VezklwB2/LoCSALB0Xnu4lc6e+7xV+zk15EI8x+1T+rbgjGQF3EOeTiXRnEeTeN85EMFyHt8yIMKkbMU0UYu1mspooxPKc/g
lzX0G6+hX1kS14hjfQ3RRguOK7FAD+ovKNe8YX38JtZXsY98lfsot0Jjdv5zB3L1yp38F4BBV0U3vx9wW27LtYv/QVjOh4r8u5AV4fEzCM+9j/zItJVixMTS
QKZUlPRWmAHDyrQM8SeZezrJHXBvh8aMB4Q7Cxq7klZjIfchZUwAmqfM/EQ56GBgOLawQ8PoWB5OfqSC4/lomolAWsvHYPRRlOBjIYJj6HwepcWwgDyfyt+V
w1jeh06dIlDY4xT1z/MGf6SNpAdo0jiwrb8NSQLAUM9sNpM6J6ITNS72+7XQJDAoKP9O5VK0rffUJVM6IdIr+Tu8pFo1oOSSPXXUqb4iqU5DK5LqdNm8StdO
5dGg+O0+HZD/04DYT8dxuV5gtugsGA99Qkcnt/dmdLkT0MXhcTUml4lOLK+veIy2bCZfRZfK3kfZcqvv7PlrxWMqbzMVVexVefMqu1TBPvLIbVvFtkEGMw5q
U0VDeTykfoKe3EsZyE8hnuIhmPMe+lFyzhpYjNhiKUKR8m2mfDH8QlVS7tqhhm5zvEA5vMC2Qcs6hYp5gqacQVyMpiJVnCTZjmWIWzkFy9hH48H08A4aidvR
SBNxG9lBBbgd00Fe3EYhj3RtTa7w2Yw+0y8RWwbIyoTzG8unUzmeT+Lpes4cSZhkNWOgyXAC8gVncvbzIOd83E/uVsdNNAWcl03MLTFLcu0iVXbDzZRnZZeg
3KX8Yib+8tISlz73QuozdfugySEcAFuTZ2MP52DyWj25h9SnNFrOCpT82X5y3vvlf4jAvQ0JpWt7qaCMoSUeuRl5nVRYknl1iefqSCdllbiuLr06Mik7N29o
dreq7FYnTMq2vNhlYQHJVlahOgmmZnkDOeVDs62codlF6pS13WriLjUZjIHXXeo0UdzpbupjWLtkrgPD9VTG8+hknk9n8AJq5TM10zAZd35WDZRgWIZ7OiwG
QjsKFukwf3FSxWvKYf3OIUCekya9KT4RLM3udI4FdI32LfUavEAPhWmjohPgqWKXqiXqUxUdiLiRKngRnciLNSPF5B7irgcDn0qAfR4/sxxuKnq5aQIvopOn
gpscwSvOFJDLNikVqrldqm4zDZFChm5X8zObu9TCTjKkOFhXl5GPl+vJs8iVm1Fa7Uw4IeVXEZocjfWWS9rXSa56J+a5sadvDjCscyHgVfDqQTiXplQkmgNv
I7RfBTzRLoNHorOEyEuwAm95alsXgXJ5t1pS79e4Up09z4GVrkrJFoEq3epWykPwySh2d6lgZ8978OqjKjWyOwq99lFhZRLmwZ8LhLzfcu3pVk2wnjVdqnVb
vdirX7u2diIdvLGukdBSQWcLqJGW4A4dpXDvupxI1YLIuoZGcojG8Fo6nVsxYg01AYmHOUwRjui1ziFvtnsapPcZFRrqLAEu6qZPaOhc7ZVNBHHHGTtN/fxx
j+yE06jTjwHAYBIFkvtwShIY5PsfJAMWITe4pXWDfWqc8jiRcj552Mrp9MMkiQmokq0scEiYmgTkEx9MYyPl86YUjfx8r7zgStKIyH9Gg/vU8j79TyA9Lu+n
/BX+LrVhMw2vKFTnQxuBpfF0odbL5KnXoPlG0HAemTRFd95cSGI/HYVrBqCBkveOyWhTi/5Z4pDLdRY/zC+nDYjfmc6ZvJw6QCQX8SArQ7YLOEkjqDG1phH9
IKRL/VB336JuS9aVqtvxdD751DNY9R36epd6kTLU3eoetVWXf6yv2/T1AX19UF+79LVbX3fp68N65KNqj/qpfnpc/Uzt0y1P6uvP9fUX+vorff2Nek0blUJS
4FGv0zD1R/WGeous/wNQSwcI/IBxSaUgAAAUSAAAUEsDBAoAAAgAAHaYc10AAAAAAAAAAAAAAAAeAAAAbmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL2phdmEv
UEsDBBQACAgIAHaYc10AAAAAAAAAAAAAAAA2AAAAbmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL2phdmEvSmF2YVJ1bnRpbWVMb2NhdG9yLmNsYXNznVf5VxvH
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
bnQvUEsBAhQAFAAICAgAd5hzXUnk6PK3EAAAvBAAABwAAAAAAAAAAAAAAAAAJQEAAGFzc2V0cy9mb250L3BpeGVsc19hdGxhcy5wbmdQSwECCgAKAAAIAAB3
mHNdAAAAAAAAAAAAAAAADQAAAAAAAAAAAAAAAAAmEgAAYXNzZXRzL2xpZ2h0L1BLAQIUABQACAgIAHeYc12oBzc4sAEAAA4CAAAVAAAAAAAAAAAAAAAAAFES
AABhc3NldHMvbGlnaHQvbG9nby5wbmdQSwECCgAKAAAIAAB2mHNdAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAABEFAAAbmV0L1BLAQIKAAoAAAgAAHaYc10A
AAAAAAAAAAAAAAARAAAAAAAAAAAAAAAAAGYUAABuZXQvbGl0ZWxhdW5jaGVyL1BLAQIKAAoAAAgAAHaYc10AAAAAAAAAAAAAAAAZAAAAAAAAAAAAAAAAAJUU
AABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvUEsBAhQAFAAICAgAdphzXQYMStkeCAAAJRAAACsAAAAAAAAAAAAAAAAAzBQAAG5ldC9saXRlbGF1bmNoZXIv
YmFja2VuZC9Cb290c3RyYXBMb2cuY2xhc3NQSwECCgAKAAAIAAB2mHNdAAAAAAAAAAAAAAAAIgAAAAAAAAAAAAAAAABDHQAAbmV0L2xpdGVsYXVuY2hlci9i
YWNrZW5kL2Rvd25sb2FkL1BLAQIUABQACAgIAHaYc10Dwg7/TwEAADoCAAA5AAAAAAAAAAAAAAAAAIMdAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvZG93
bmxvYWQvRG93bmxvYWRFeGNlcHRpb24uY2xhc3NQSwECFAAUAAgICAB2mHNdTKkB6qUDAAAyCAAANAAAAAAAAAAAAAAAAAA5HwAAbmV0L2xpdGVsYXVuY2hl
ci9iYWNrZW5kL2Rvd25sb2FkL0Rvd25sb2FkRmlsZS5jbGFzc1BLAQIUABQACAgIAHaYc11BdD/2zAAAABkBAAA4AAAAAAAAAAAAAAAAAEAjAABuZXQvbGl0
ZWxhdW5jaGVyL2JhY2tlbmQvZG93bmxvYWQvRG93bmxvYWRQcm9ncmVzcy5jbGFzc1BLAQIUABQACAgIAHaYc125cQ+xSwMAABEIAABJAAAAAAAAAAAAAAAA
AHIkAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvZG93bmxvYWQvRG93bmxvYWRTZXJ2aWNlJFZhbGlkYXRpb25TdW1tYXJ5LmNsYXNzUEsBAhQAFAAICAgA
dphzXfyAcUmlIAAAFEgAADcAAAAAAAAAAAAAAAAANCgAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9kb3dubG9hZC9Eb3dubG9hZFNlcnZpY2UuY2xhc3NQ
SwECCgAKAAAIAAB2mHNdAAAAAAAAAAAAAAAAHgAAAAAAAAAAAAAAAAA+SQAAbmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL2phdmEvUEsBAhQAFAAICAgAdphz
XZHW1TIuBwAA6g4AADYAAAAAAAAAAAAAAAAAekkAAG5ldC9saXRlbGF1bmNoZXIvYmFja2VuZC9qYXZhL0phdmFSdW50aW1lTG9jYXRvci5jbGFzc1BLAQIU
ABQACAgIAHaYc10ThWOAtwoAADgUAABJAAAAAAAAAAAAAAAAAAxRAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvamF2YS9KYXZhUnVudGltZU1hbmlmZXN0
U2VydmljZSRKc29uUGFyc2VyLmNsYXNzUEsBAhQAFAAICAgAdphzXanu0cNuDgAAUxwAAD4AAAAAAAAAAAAAAAAAOlwAAG5ldC9saXRlbGF1bmNoZXIvYmFj
a2VuZC9qYXZhL0phdmFSdW50aW1lTWFuaWZlc3RTZXJ2aWNlLmNsYXNzUEsBAhQAFAAICAgAdphzXdHD8xgzAwAAFgYAADYAAAAAAAAAAAAAAAAAFGsAAG5l
dC9saXRlbGF1bmNoZXIvYmFja2VuZC9qYXZhL0phdmFSdW50aW1lUGFja2FnZS5jbGFzc1BLAQIUABQACAgIAHaYc10VYJIuWhYAAJwwAAA2AAAAAAAAAAAA
AAAAAKtuAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvamF2YS9KYXZhUnVudGltZVNlcnZpY2UuY2xhc3NQSwECCgAKAAAIAAB2mHNdAAAAAAAAAAAAAAAA
IgAAAAAAAAAAAAAAAABphQAAbmV0L2xpdGVsYXVuY2hlci9iYWNrZW5kL3BsYXRmb3JtL1BLAQIUABQACAgIAHaYc12iU/aBywMAAJ0HAAA1AAAAAAAAAAAA
AAAAAKmFAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvcGxhdGZvcm0vTGF1bmNoZXJQYXRocy5jbGFzc1BLAQIUABQACAgIAHaYc10AE1gwdwgAAHsQAAAv
AAAAAAAAAAAAAAAAANeJAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvcGxhdGZvcm0vT1NVdGlscy5jbGFzc1BLAQIUABQACAgIAHaYc12UgBCVYQQAACkJ
AAA3AAAAAAAAAAAAAAAAAKuSAABuZXQvbGl0ZWxhdW5jaGVyL2JhY2tlbmQvcGxhdGZvcm0vT3BlcmF0aW5nU3lzdGVtLmNsYXNzUEsBAgoACgAACAAAdphz
XQAAAAAAAAAAAAAAABsAAAAAAAAAAAAAAAAAcZcAAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0cmFwL1BLAQIUABQACAgIAHaYc11v88N6aAIAAEgFAAA/AAAA
AAAAAAAAAAAAAKqXAABuZXQvbGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9Cb290c3RyYXAkQm9vdHN0cmFwU2NlbmUkU3RhZ2UuY2xhc3NQSwECFAAUAAgICAB2
mHNdSb2hp/IHAADyEAAAOQAAAAAAAAAAAAAAAAB/mgAAbmV0L2xpdGVsYXVuY2hlci9ib290c3RyYXAvQm9vdHN0cmFwJEJvb3RzdHJhcFNjZW5lLmNsYXNz
UEsBAhQAFAAICAgAdphzXd7z57ssBAAAbwkAADYAAAAAAAAAAAAAAAAA2KIAAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0cmFwL0Jvb3RzdHJhcCRCb290c3Ry
YXBVaS5jbGFzc1BLAQIUABQACAgIAHaYc13v4ZYT9QQAABQLAAAqAAAAAAAAAAAAAAAAAGinAABuZXQvbGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9Cb290c3Ry
YXAuY2xhc3NQSwECFAAUAAgICAB2mHNdv+PTqqsMAADoGwAAMQAAAAAAAAAAAAAAAAC1rAAAbmV0L2xpdGVsYXVuY2hlci9ib290c3RyYXAvQm9vdHN0cmFw
QmFja2VuZC5jbGFzc1BLAQIUABQACAgIAHaYc13e3Kaf8AAAAG8BAAAzAAAAAAAAAAAAAAAAAL+5AABuZXQvbGl0ZWxhdW5jaGVyL2Jvb3RzdHJhcC9Cb290
c3RyYXBFeGNlcHRpb24uY2xhc3NQSwECFAAUAAgICAB2mHNdZAKC17ALAAB3GwAANwAAAAAAAAAAAAAAAAAQuwAAbmV0L2xpdGVsYXVuY2hlci9ib290c3Ry
YXAvTGF1bmNoZXJJbnN0YWxsU2VydmljZS5jbGFzc1BLAQIUABQACAgIAHaYc104yrbnvQoAALIWAAAxAAAAAAAAAAAAAAAAACXHAABuZXQvbGl0ZWxhdW5j
aGVyL2Jvb3RzdHJhcC9MYXVuY2hlck1hbmlmZXN0LmNsYXNzUEsBAhQAFAAICAgAdphzXXAgdMMSDQAAZRwAADgAAAAAAAAAAAAAAAAAQdIAAG5ldC9saXRl
bGF1bmNoZXIvYm9vdHN0cmFwL0xhdW5jaGVyTWFuaWZlc3RTZXJ2aWNlLmNsYXNzUEsBAhQAFAAICAgAdphzXW5b2Fh8AgAAsAUAAC0AAAAAAAAAAAAAAAAA
ud8AAG5ldC9saXRlbGF1bmNoZXIvYm9vdHN0cmFwL01hbmlmZXN0TG9hZC5jbGFzc1BLAQIKAAoAAAgAAHaYc10AAAAAAAAAAAAAAAAUAAAAAAAAAAAAAAAA
AJDiAABuZXQvbGl0ZWxhdW5jaGVyL3VpL1BLAQIUABQACAgIAHaYc10mua9Q5QUAAJwKAAAjAAAAAAAAAAAAAAAAAMLiAABuZXQvbGl0ZWxhdW5jaGVyL3Vp
L0FwcFdpbmRvdy5jbGFzc1BLAQIUABQACAgIAHaYc13/V4+8+AMAABwHAAAhAAAAAAAAAAAAAAAAAPjoAABuZXQvbGl0ZWxhdW5jaGVyL3VpL0hvdHNwb3Qu
Y2xhc3NQSwECFAAUAAgICAB2mHNdsY2gRgUDAACzBQAAJQAAAAAAAAAAAAAAAAA/7QAAbmV0L2xpdGVsYXVuY2hlci91aS9Nb3VzZUN1cnNvci5jbGFzc1BL
AQIUABQACAgIAHaYc13bIHuzxQMAAKYHAAAkAAAAAAAAAAAAAAAAAJfwAABuZXQvbGl0ZWxhdW5jaGVyL3VpL01vdXNlU3RhdGUuY2xhc3NQSwECFAAUAAgI
CAB2mHNdgeqpIE0CAADTAwAAIQAAAAAAAAAAAAAAAACu9AAAbmV0L2xpdGVsYXVuY2hlci91aS9QYWxldHRlLmNsYXNzUEsBAhQAFAAICAgAdphzXfmG0Y8L
AQAAzwEAAC4AAAAAAAAAAAAAAAAASvcAAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxCdXR0b24kUmVuZGVyZXIuY2xhc3NQSwECFAAUAAgICAB2mHNd2Duq
OIICAADgBAAAKwAAAAAAAAAAAAAAAACx+AAAbmV0L2xpdGVsYXVuY2hlci91aS9QaXhlbEJ1dHRvbiRTdGF0ZS5jbGFzc1BLAQIUABQACAgIAHaYc101Xm9V
gwUAAJgKAAAlAAAAAAAAAAAAAAAAAIz7AABuZXQvbGl0ZWxhdW5jaGVyL3VpL1BpeGVsQnV0dG9uLmNsYXNzUEsBAhQAFAAICAgAdphzXSvaUvR2AwAAtAYA
ADIAAAAAAAAAAAAAAAAAYgEBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxDYW52YXMkSW5wdXRIYW5kbGVyLmNsYXNzUEsBAhQAFAAICAgAdphzXSr7nku2
BwAAow4AACUAAAAAAAAAAAAAAAAAOAUBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxDYW52YXMuY2xhc3NQSwECFAAUAAgICAB2mHNdrkooDcINAABgGgAA
JwAAAAAAAAAAAAAAAABBDQEAbmV0L2xpdGVsYXVuY2hlci91aS9QaXhlbEdyYXBoaWNzLmNsYXNzUEsBAhQAFAAICAgAdphzXbHzTFPCBwAArhEAACYAAAAA
AAAAAAAAAAAAWBsBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxQYWludGVyLmNsYXNzUEsBAhQAFAAICAgAdphzXVmaPNP2AQAAuwMAACYAAAAAAAAAAAAA
AAAAbiMBAG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxTdXJmYWNlLmNsYXNzUEsBAhQAFAAICAgAdphzXTru2xJnAgAAtAQAAC0AAAAAAAAAAAAAAAAAuCUB
AG5ldC9saXRlbGF1bmNoZXIvdWkvUGl4ZWxUZXh0JEFsaWdubWVudC5jbGFzc1BLAQIUABQACAgIAHaYc12OrgBgTAcAAMcNAAAjAAAAAAAAAAAAAAAAAHoo
AQBuZXQvbGl0ZWxhdW5jaGVyL3VpL1BpeGVsVGV4dC5jbGFzc1BLAQIUABQACAgIAHaYc13vdnVuIgIAAFQDAAAlAAAAAAAAAAAAAAAAABcwAQBuZXQvbGl0
ZWxhdW5jaGVyL3VpL1Byb2dyZXNzQmFyLmNsYXNzUEsBAhQAFAAICAgAdphzXYxucALBAAAA8QAAACYAAAAAAAAAAAAAAAAAjDIBAG5ldC9saXRlbGF1bmNo
ZXIvdWkvVGFza1Byb2dyZXNzLmNsYXNzUEsBAhQAFAAICAgAdphzXTwuS4+MBgAAFA4AACYAAAAAAAAAAAAAAAAAoTMBAG5ldC9saXRlbGF1bmNoZXIvdWkv
V2luZG93Q2hyb21lLmNsYXNzUEsBAgoACgAACAAAdphzXQAAAAAAAAAAAAAAABkAAAAAAAAAAAAAAAAAgToBAG5ldC9saXRlbGF1bmNoZXIvdWkvdGV4dC9Q
SwECFAAUAAgICAB2mHNdQQw2T9ACAACqBAAAKgAAAAAAAAAAAAAAAAC4OgEAbmV0L2xpdGVsYXVuY2hlci91aS90ZXh0L0dseXBoTGF5b3V0LmNsYXNzUEsB
AhQAFAAICAgAdphzXZnSXCcxAwAARQcAAC0AAAAAAAAAAAAAAAAA4D0BAG5ldC9saXRlbGF1bmNoZXIvdWkvdGV4dC9UZXh0Rm9udCRHbHlwaC5jbGFzc1BL
AQIUABQACAgIAHaYc12wehGgjDwAAPt7AAAnAAAAAAAAAAAAAAAAAGxBAQBuZXQvbGl0ZWxhdW5jaGVyL3VpL3RleHQvVGV4dEZvbnQuY2xhc3NQSwECFAAU
AAgICAB2mHNdyNZo/qsGAAAHDAAALQAAAAAAAAAAAAAAAABNfgEAbmV0L2xpdGVsYXVuY2hlci91aS90ZXh0L1RleHRSYXN0ZXJpemVyLmNsYXNzUEsFBgAA
AAA+AD4A7RQAAFOFAQAAAA==
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
