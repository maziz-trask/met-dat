import { Alert, Button } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

export function NotAllowed() {
  const { t } = useTranslation();
  return (
    <Alert severity="warning" action={<Button component={Link} to="/tests" color="inherit" size="small">{t('notAllowed.back')}</Button>}>
      {t('notAllowed.text')}
    </Alert>
  );
}
