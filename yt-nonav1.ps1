param(
    [Parameter(Mandatory=$true)]
    [string]$Url,

    [string]$Ext = "mp4",

    # プレイリスト番号のゼロ埋め桁数（プレイリスト時のみ）
    [int]$Pad = 2
)

if ($Ext -eq "mp4") {
    $format = 'bestvideo[ext=mp4][vcodec^=avc1]+bestaudio[ext=m4a]/best[ext=mp4][vcodec^=avc1]'
} elseif ($Ext -eq "webm") {
    $format = 'bestvideo[ext=webm][vcodec^=vp9]+bestaudio[ext=webm]/best[ext=webm][vcodec^=vp9]'
} else {
    $format = 'bestvideo[vcodec!=av01]+bestaudio/best[vcodec!=av01]'
}

# URL に playlist パラメータが含まれているかチェック
if ($Url -match '([?&])list=') {
    # プレイリスト扱い：番号-タイトル
    $outTemplate = "%(playlist_index)0${Pad}d - %(title)s.%(ext)s"
    $playlistFlag = "--yes-playlist"
} else {
    # 単体動画扱い：タイトルのみ（先頭に余分なハイフンが出ないようにする）
    $outTemplate = "%(title)s.%(ext)s"
    $playlistFlag = "--no-playlist"
}

Write-Host "format: $format"
Write-Host "output template: $outTemplate"
Write-Host "playlist flag: $playlistFlag"
Write-Host "Url: $Url"

# 実行
yt-dlp -f $format --merge-output-format $Ext -o $outTemplate $playlistFlag $Url