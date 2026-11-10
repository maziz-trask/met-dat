import { Alert, Link, List, ListItem, ListItemText, Paper, Typography } from '@mui/material';

export function About() {
  return (
    <Paper sx={{ p: { xs: 2, md: 4 }, maxWidth: 900 }}>
      <Typography variant="h4" gutterBottom>About this demo</Typography>
      <Alert severity="info" sx={{ mb: 2 }}>Demonstration system with synthetic data (benchmark MET-DAT 2026). Not for production use.</Alert>
      <Typography paragraph>
        MET-DAT manages technical tests performed on vehicles: overview with server-side search/filter/sort/pagination, create/edit, detail with audit data,
        attachments, status workflow with the completion rule, CSV export and test-type management.
      </Typography>
      <Typography variant="h6">Roles (verified on the server for every request)</Typography>
      <List dense>
        <ListItem><ListItemText primary="Reader" secondary="View/search/filter tests, open details, view and download attachments." /></ListItem>
        <ListItem><ListItemText primary="Editor" secondary="Everything a Reader can, plus create/edit tests, upload/delete attachments, change status, manage test types." /></ListItem>
      </List>
      <Typography variant="h6">Completion rule (F06)</Typography>
      <Typography paragraph>
        A test can only become <em>Completed</em> if its result is exactly <em>Passed</em> or <em>Failed</em> and at least one attachment exists.
        The last attachment of a Completed test cannot be deleted – reopen the test (Draft / In Progress) first.
        Some seeded legacy records are already Completed with other results; they are shown unchanged (see docs/seed-data-validation-report.md).
      </Typography>
      <Typography variant="h6">Calendar weeks</Typography>
      <Typography paragraph>
        Seeded tests carry an ISO calendar week (e.g. KT08/26 = Monday–Sunday of ISO week 8, 2026). A date filter matches tests whose week overlaps the From–To range.
      </Typography>
      <Typography variant="h6">API</Typography>
      <Typography>
        Interactive OpenAPI documentation: <Link href="/swagger-ui.html" target="_blank" rel="noreferrer">/swagger-ui.html</Link> · raw spec:{' '}
        <Link href="/v3/api-docs" target="_blank" rel="noreferrer">/v3/api-docs</Link>
      </Typography>
    </Paper>
  );
}
