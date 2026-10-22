import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from "react-router";
import '@mantine/core/styles.css'
import '@mantine/dates/styles.css'
import '@mantine/notifications/styles.css'
import { Center, createTheme, MantineProvider, Text } from '@mantine/core';
import { DatesProvider } from '@mantine/dates';
import { Notifications } from '@mantine/notifications';
import dayjs from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import 'dayjs/locale/pt-br';
import AppRoutes from './AppRoutes';
import { sessionService } from './instance';
import { SessionContext } from './session/session.context';
import { z } from 'zod/v4';

// The DateInput parses what is typed (dd/mm/yyyy) with the format of the input
dayjs.extend(customParseFormat)

// The Content-Security-Policy of the Caddyfile forbids eval, so zod validates without compiling its schemas, and does
// not even try (the try is reported as a violation)
z.config({ jitless: true })

const theme = createTheme({})

const root = createRoot(document.getElementById('root')!)

// Nothing is rendered before the session exists: without it, every request to the API would be refused
sessionService.start()
    .then(session => root.render(
        <StrictMode>
            <MantineProvider theme={theme}>
                <DatesProvider settings={{ locale: 'pt-br' }}>
                    <Notifications/>
                    <SessionContext value={session}>
                        <BrowserRouter>
                            <AppRoutes/>
                        </BrowserRouter>
                    </SessionContext>
                </DatesProvider>
            </MantineProvider>
        </StrictMode>
    ))
    .catch(e => {
        console.error("Cannot start session. Error: " + e)
        root.render(
            <MantineProvider theme={theme}>
                <Center h="100vh">
                    <Text>Não foi possível entrar. Tente novamente mais tarde.</Text>
                </Center>
            </MantineProvider>
        )
    })
