import { Alert, Link, List, ListItem, ListItemText, Paper, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

export function About() {
  const { t } = useTranslation();
  return (
    <Paper sx={{ p: { xs: 2, md: 4 }, maxWidth: 900 }}>
      <Typography variant="h4" gutterBottom>{t('about.title')}</Typography>
      <Alert severity="info" sx={{ mb: 2 }}>{t('about.alert')}</Alert>
      <Typography paragraph>{t('about.intro')}</Typography>
      <Typography variant="h6">{t('about.rolesTitle')}</Typography>
      <List dense>
        <ListItem><ListItemText primary={t('roles.READER')} secondary={t('about.reader')} /></ListItem>
        <ListItem><ListItemText primary={t('roles.EDITOR')} secondary={t('about.editor')} /></ListItem>
      </List>
      <Typography variant="h6">{t('about.ruleTitle')}</Typography>
      <Typography paragraph>{t('about.ruleText')}</Typography>
      <Typography variant="h6">{t('about.weeksTitle')}</Typography>
      <Typography paragraph>{t('about.weeksText')}</Typography>
      <Typography variant="h6">{t('about.apiTitle')}</Typography>
      <Typography>
        {t('about.apiText')}: <Link href="/swagger-ui.html" target="_blank" rel="noreferrer">/swagger-ui.html</Link> · {t('about.rawSpec')}:{' '}
        <Link href="/v3/api-docs" target="_blank" rel="noreferrer">/v3/api-docs</Link>
      </Typography>
    </Paper>
  );
}
