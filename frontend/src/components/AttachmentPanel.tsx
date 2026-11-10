import { Box, Chip, IconButton, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Tooltip, Typography, Button } from '@mui/material';
import Delete from '@mui/icons-material/Delete';
import Download from '@mui/icons-material/Download';
import { useTranslation } from 'react-i18next';
import type { Attachment } from '../models/types';
import { formatBytes, formatDateTime } from '../utils/format';

interface Props {
  attachments: Attachment[];
  canEdit: boolean;
  showNeedHint: boolean;
  uploading: boolean;
  allowedExtensions: string[];
  maxFileSizeBytes: number;
  onPick: (files: FileList | null, resetInput: () => void) => void;
  onDownload: (a: Attachment) => void;
  onDelete: (a: Attachment) => void;
}

/** Attachment list of a test (F07). Upload/delete controls render only for Editors; the backend enforces it regardless. */
export function AttachmentPanel({ attachments, canEdit, showNeedHint, uploading, allowedExtensions, maxFileSizeBytes, onPick, onDownload, onDelete }: Props) {
  const { t } = useTranslation();
  return (
    <Paper sx={{ p: { xs: 2, md: 3 } }}>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 1 }}>
        <Typography variant="h6" component="h2">{t('detail.attachments', { count: attachments.length })}</Typography>
        {canEdit && (
          <Button variant="outlined" component="label" size="small" disabled={uploading}>{uploading ? t('detail.uploading') : t('detail.upload')}
            <input hidden type="file" multiple data-testid="upload-input" accept={allowedExtensions.map((e) => '.' + e).join(',')}
              onChange={(e) => onPick(e.target.files, () => { e.target.value = ''; })} />
          </Button>
        )}
      </Stack>
      {attachments.length === 0 ? (
        <Typography color="text.secondary">{t('detail.noAttachments')} {showNeedHint && t('detail.needAttachment')}</Typography>
      ) : (
        <Box sx={{ overflowX: 'auto' }}>
          <Table size="small" aria-label={t('detail.attachments', { count: attachments.length })}>
            <TableHead><TableRow>
              <TableCell>{t('detail.colFile')}</TableCell><TableCell>{t('detail.colSize')}</TableCell><TableCell>{t('detail.colUploaded')}</TableCell>
              <TableCell align="right">{t('common.actions')}</TableCell>
            </TableRow></TableHead>
            <TableBody>
              {attachments.map((a) => (
                <TableRow key={a.id}>
                  <TableCell>
                    {a.originalFilename}
                    {a.synthetic && <Tooltip title={t('detail.syntheticHint')}><Chip size="small" label={t('common.synthetic')} sx={{ ml: 1 }} /></Tooltip>}
                  </TableCell>
                  <TableCell>{formatBytes(a.fileSize)}</TableCell>
                  <TableCell>{formatDateTime(a.uploadedAt)} · {a.uploadedBy}</TableCell>
                  <TableCell align="right">
                    <Tooltip title={t('detail.download', { name: a.originalFilename })}>
                      <IconButton size="small" onClick={() => onDownload(a)} aria-label={t('detail.download', { name: a.originalFilename })}><Download fontSize="small" /></IconButton>
                    </Tooltip>
                    {canEdit && (
                      <Tooltip title={t('common.delete')}>
                        <IconButton size="small" color="error" onClick={() => onDelete(a)} aria-label={t('detail.remove', { name: a.originalFilename })}><Delete fontSize="small" /></IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Box>
      )}
      {canEdit && (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
          {t('detail.limits', { ext: allowedExtensions.join(', '), size: formatBytes(maxFileSizeBytes) })}
        </Typography>
      )}
    </Paper>
  );
}
