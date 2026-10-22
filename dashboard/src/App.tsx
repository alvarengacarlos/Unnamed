import { AppShell, Burger, Button, Center, Group, Image, Loader, NavLink, Text } from '@mantine/core';
import { Suspense } from 'react';
import { useDisclosure } from '@mantine/hooks';
import { Link, Outlet, useLocation } from 'react-router';
import { RoutePath } from './common/common.type';
import { useSession } from './session/session.context';
import { sessionService } from './instance';

export default function App() {
    const session = useSession()

    const [isOpen, { toggle, close }] = useDisclosure()

    const location = useLocation()

    const data = [
        {
            label: 'Dashboard',
            path: RoutePath.DASHBOARD
        },
        {
            label: 'Mensalidades',
            path: RoutePath.TUITION
        },
        {
            label: 'Graus',
            path: RoutePath.RANK
        },
        {
            label: 'Membros',
            path: RoutePath.MEMBER
        },
        {
            label: 'Turmas',
            path: RoutePath.CLAZZ
        },
        {
            label: 'Graduações',
            path: RoutePath.GRADING
        },
        {
            label: 'Torneios',
            path: RoutePath.TOURNAMENT
        }
    ]

    // The active item comes from the URL, so it is right after a reload or a link opened directly
    function isActive(path: RoutePath): boolean {
        if (path === RoutePath.DASHBOARD) {
            return location.pathname === path
        }

        return location.pathname.startsWith(path)
    }

    const navLinks = data.map((item) => (
        <NavLink
            key={item.label}
            component={Link}
            to={item.path}
            active={isActive(item.path)}
            label={item.label}
            // On mobile the navbar covers the page, so it closes once a page is chosen
            onClick={close}
        />
    ))

    return (
        <AppShell
            padding="md"
            header={{ height: 60 }}
            navbar={{
                width: 300,
                breakpoint: 'sm',
                collapsed: { mobile: !isOpen },
            }}
        >
            <AppShell.Header>
            <Group h="100%" px="md" justify="space-between">
                <Group>
                    <Burger
                        opened={isOpen}
                        onClick={toggle}
                        hiddenFrom="sm"
                        size="sm"
                    />
                    <Image src="/favicon.png" alt="Logo" h={40} w="auto" fit="contain"/>
                </Group>
                <Group>
                    <Text>{session.name}</Text>
                    <Button variant="default" onClick={() => sessionService.end()}>Sair</Button>
                </Group>
            </Group>
            </AppShell.Header>

            <AppShell.Navbar>{navLinks}</AppShell.Navbar>

            <AppShell.Main>
            {/* The pages are loaded on demand, the first time they are opened */}
            <Suspense fallback={<Center my="md"><Loader/></Center>}>
                <Outlet/>
            </Suspense>
            </AppShell.Main>
        </AppShell>
    )
}
