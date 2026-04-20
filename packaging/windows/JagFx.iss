#ifndef AppVersion
  #define AppVersion "0.0.0-dev"
#endif
#ifndef SourceDir
  #define SourceDir "."
#endif
#ifndef OutputDir
  #define OutputDir "."
#endif
#ifndef IconFile
  #define IconFile "..\..\assets\jagfx-icon.ico"
#endif

[Setup]
AppId={{D06A3876-2C1F-4F43-BD0F-D03A328D9E7F}
AppName=JagFx
AppVersion={#AppVersion}
AppPublisher=JagFx Contributors
DefaultDirName={autopf}\JagFx
DefaultGroupName=JagFx
DisableProgramGroupPage=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
OutputDir={#OutputDir}
OutputBaseFilename=JagFx-{#AppVersion}-win-x64-setup
Compression=lzma
SolidCompression=yes
WizardStyle=modern
SetupIconFile={#IconFile}
UninstallDisplayIcon={app}\JagFx.Desktop.exe

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\JagFx"; Filename: "{app}\JagFx.Desktop.exe"
Name: "{autodesktop}\JagFx"; Filename: "{app}\JagFx.Desktop.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\JagFx.Desktop.exe"; Description: "{cm:LaunchProgram,JagFx}"; Flags: nowait postinstall skipifsilent
